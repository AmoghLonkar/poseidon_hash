package poseidon

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._ 

import scala.collection.mutable.ArrayBuffer
import scala.collection.script.Message

class MerkleTreeModelTester extends FreeSpec with ChiselScalatestTester {
    "Simple 2-to-1 Merkle Tree should return the proper hash" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val msg1 = Message("", 3)
        val msg2 = Message("", 3)

        val inSeq = Seq(msg1, msg2)

        val perm1 = new PoseidonModel(p)
        val perm2 = new PoseidonModel(p)

        val hash1: BigInt = perm1(msg1) 
        val hash2: BigInt = perm2(msg2)

        val finalIn: String = hash1.toString + hash2.toString

        val final_perm = new PoseidonModel(p)
        val exp_out: BigInt = final_perm(Message(finalIn, 3))

        val m = MerkleParams(p)
        val mTree = new MerkleTreeModel(m)
        val model_out: BigInt = mTree(inSeq)
        assert(model_out == exp_out)
    }
}