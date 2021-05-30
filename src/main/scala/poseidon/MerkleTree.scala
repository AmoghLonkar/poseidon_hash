package poseidon

import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer
import scala.annotation.switch 

object MerkleTree {
    //States
    val idle :: loading :: hashing :: Nil = Enum(3)
}

class HWNodeIO(m: MerkleParams) extends Bundle {
    val in_data: UInt = Input(UInt())
    val children = Input(Vec(m.numChild, UInt((log2Ceil(m.numNodes)).W)))
    val hash: UInt = Output(UInt((m.p.hashLen * 8).W))
    val hashReady: Bool = Output(Bool())
}

class HWNode (m: MerkleParams) extends Module {
    val io = IO(new HWNodeIO(m))

    val permutation = Module(new Poseidon(m.p))
    permutation.io.msg.valid := true.B
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
    
    //Make tree connections- Hard coded for now
    tree_io(0).children := VecInit(Seq(1.U, 2.U))
    tree_io(1).children := VecInit(Seq(1.U, 1.U))
    tree_io(2).children := VecInit(Seq(2.U, 2.U))
    tree_io(0).in_data := 0.U
    tree_io(1).in_data := 0.U
    tree_io(2).in_data := 0.U

    //Counter initialization
    val (cycles, done) = Counter(0 until (m.numNodes - 1)*((m.p.Rf + m.p.Rp)*(3 + m.p.t*m.p.t/m.p.parallelism)), state === MerkleTree.hashing)
    val (loadingCount, loadingDone) = Counter(0 until m.numInputs, state === MerkleTree.loading)
    val (hashCount, hashDone) = Counter(0 until ((m.p.Rf + m.p.Rp)*(3 + m.p.t*m.p.t/m.p.parallelism)), state === MerkleTree.hashing)
    val (nodeCount, nodesDone) = Counter(0 until m.numNodes - 1, hashDone && (state === MerkleTree.hashing))
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

            tree_io(loadingCount + 1.U).in_data := io.msg.bits
            inSeq(loadingCount) := io.msg.bits

            when(loadingDone){
                state := MerkleTree.hashing
            }.otherwise{
                state := MerkleTree.loading
            }
        }

        is(MerkleTree.hashing){
            //Wait for hash to compute
            when(tree_io(nodeCount).hashReady && tree_io(nodeCount + 1.U).hashReady){
                tree_io(0).in_data := Cat(tree_io(1).hash, tree_io(2).hash)
            }
            when(nodesDone){
                state := MerkleTree.idle
            }.otherwise{
                state := MerkleTree.hashing 
            }
        }
    }

    io.digest.valid := RegNext(done)
    io.digest.bits := tree(0).io.hash
}

