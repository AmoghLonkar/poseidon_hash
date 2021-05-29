package poseidon

import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer
import scala.annotation.switch 

object MerkleTree {
    //States
    val idle :: loading :: hashing :: Nil = Enum(3)
}

class HWNode (m: MerkleParams) extends Module {
    val io = IO(new Bundle {
        val in_data: UInt = Input(UInt((m.p.hashLen * 8).W))
        val children = Input(Vec(m.numChild, UInt((log2Ceil(m.numNodes)).W)))
        val hash: UInt = Output(UInt((m.p.hashLen * 8).W))
    })

    val permutation = Module(new Poseidon(m.p))
    permutation.io.msg.valid := true.B
    permutation.io.msg.bits := io.in_data
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

    //Counter initialization
    val (loadingCount, loadingDone) = Counter(0 until m.numInputs, state === MerkleTree.loading)

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

        }
    }

    io.digest.valid := false.B
    io.digest.bits := inSeq(0)
}

