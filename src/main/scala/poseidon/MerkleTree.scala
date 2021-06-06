package poseidon

import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer
import scala.annotation.switch
import scala.math._ 

object MerkleTree {
    //States
    val idle :: loading :: hashing :: Nil = Enum(3)
}

class HWNodeIO(m: MerkleParams) extends Bundle {
    val in_data: UInt = Input(UInt())
    val dataReady: Bool = Input(Bool())
    val children = Input(Vec(m.treeRadix, UInt((log2Ceil(m.numNodes)).W)))
    val hash: UInt = Output(UInt((m.p.hashLen * 8).W))
    val hashReady: Bool = Output(Bool())
}

class HWNode (m: MerkleParams) extends Module {
    val io = IO(new HWNodeIO(m))

    val permutation = Module(new Poseidon(m.p))
    
    permutation.io.msg.valid := io.dataReady
    val inReady = permutation.io.msg.ready
    permutation.io.msg.bits := io.in_data 
    permutation.io.digest.ready := true.B
    io.hashReady := permutation.io.digest.valid
    io.hash := permutation.io.digest.bits
}

class MerkleTreeIO(m: MerkleParams) extends Bundle {
    val msg = Flipped(Decoupled(UInt((m.p.msgLen * 8).W)))
    val digest = Decoupled(UInt((m.p.hashLen * 8).W)) 
    override def cloneType = (new MerkleTreeIO(m)).asInstanceOf[this.type]
}

class MerkleTree(m: MerkleParams) extends Module {
    val io = IO(new MerkleTreeIO(m))

    val state = RegInit(MerkleTree.idle)
    io.msg.ready := (state === (MerkleTree.idle))
    //To hold inputs to leaf nodes
    val inSeq = Reg(Vec(m.numInputs, UInt((m.p.msgLen*8).W)))

    //Tree as a vector of nodes
    val tree = for (i <- 0 until m.numNodes) yield
    {
        val node = Module(new HWNode(m))
        node
    }   
    val tree_io = VecInit(tree.map(_.io))
    
    val outbits = RegInit(0.U((m.p.msgLen*8).W))
    
    //Make tree connections
    for(i <- 0 until m.numNodes){
        if(i < m.numNodes - m.numInputs){
            tree_io(i).children := VecInit(Seq.tabulate(m.treeRadix)(j => (m.treeRadix*i + j+1).U))
            tree_io(0).in_data := outbits
        } 
        else {
            tree_io(i).children := VecInit(Seq.fill(m.treeRadix)(i.U))
            tree_io(i).in_data := 0.U
        }

        tree_io(i).dataReady := 0.B
    }

    //Counter initialization
    val treeHeight = (log10(m.numNodes)/log10(m.treeRadix)).ceil.toInt
    val (cycles, done) = Counter(0 until (treeHeight)*((m.p.Rf + m.p.Rp)*(3 + m.p.t*m.p.t/m.p.matMulParallelism)), state === MerkleTree.hashing)
    val (loadingCount, loadingDone) = Counter(0 until m.numInputs, state === MerkleTree.loading)
    val (hashCount, hashDone) = Counter(0 until ((m.p.Rf + m.p.Rp)*(3 + m.p.t*m.p.t/m.p.matMulParallelism)), state === MerkleTree.hashing)
    val (nodeCount, nodesDone) = Counter(  m.numNodes - m.numInputs - 1 to 0 by -1, hashDone && (state === MerkleTree.hashing))
    
    switch(state){
        is(MerkleTree.idle) {
            inSeq := VecInit(Seq.fill(m.numInputs)(0.U))
            
            when(io.msg.valid && io.msg.ready){
              state := MerkleTree.loading
            }.otherwise{
              state := MerkleTree.idle
            }
        }

        is(MerkleTree.loading){
            inSeq(loadingCount) := io.msg.bits

            when(loadingDone){
                state := MerkleTree.hashing
            }.otherwise{
                state := MerkleTree.loading
            }
        }

        is(MerkleTree.hashing){
            //Wait for hash to compute
            for(i <- 0 until m.numNodes){
                if(i >= m.numNodes - m.numInputs){
                    tree_io(i).in_data := inSeq(i - 1)
                }

                tree_io(i).dataReady := 1.B
            }

            switch(m.treeRadix.U){
                is(2.U){
                    when(tree_io(tree_io(nodeCount).children(0)).hashReady && tree_io(tree_io(nodeCount).children(1)).hashReady){
                        outbits:= tree_io(tree_io(nodeCount).children(0)).hash ^ tree_io(tree_io(nodeCount).children(1)).hash
                    }
                }

                is(4.U){
                    if(m.numInputs >= 4 ){
                        when(tree_io(tree_io(nodeCount).children(0)).hashReady && tree_io(tree_io(nodeCount).children(1)).hashReady && tree_io(tree_io(nodeCount).children(2)).hashReady && tree_io(tree_io(nodeCount).children(3)).hashReady){
                            outbits:= tree_io(tree_io(nodeCount).children(0)).hash ^ tree_io(tree_io(nodeCount).children(1)).hash ^ tree_io(tree_io(nodeCount).children(2)).hash ^ tree_io(tree_io(nodeCount).children(3)).hash
                        }
                    }
                }
            }
            
            when(done){
                state := MerkleTree.idle
            }.otherwise{
                state := MerkleTree.hashing 
            }
        }
    }

    io.digest.valid := RegNext(done)
    io.digest.bits := tree(0).io.hash
}

