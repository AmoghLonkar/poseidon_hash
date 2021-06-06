package poseidon

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._ 

import scala.collection.mutable.ArrayBuffer

class MerkleTreeModelTester extends FreeSpec with ChiselScalatestTester {
    "Simple 2-to-1 Merkle Tree should return the proper hash" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val msg1 = Message("a", 3)
        val msg2 = Message("b", 3)

        val inSeq = Seq(msg1, msg2)

        val perm1 = new PoseidonModel(p)
        val perm2 = new PoseidonModel(p)

        val hash1: BigInt = perm1(msg1) 
        val hash2: BigInt = perm2(msg2)

        val finalIn: String = (hash1 ^ hash2).toString
        val final_perm = new PoseidonModel(p)
        val exp_out: BigInt = final_perm(Message(finalIn, 3))
        val m = MerkleParams(p, 2)
        val mTree = new MerkleTreeModel(m)
        val model_out: BigInt = mTree(inSeq)
        assert(model_out == exp_out)
    }

    "Simple 4-to-1 Merkle Tree should return the proper hash" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val msg1 = Message("a", 3)
        val msg2 = Message("b", 3)
        val msg3 = Message("c", 3)
        val msg4 = Message("d", 3)

        val inSeq = Seq(msg1, msg2, msg3, msg4)
        val perm1 = new PoseidonModel(p)
        val perm2 = new PoseidonModel(p)
        val perm3 = new PoseidonModel(p)
        val perm4 = new PoseidonModel(p)

        val hash1: BigInt = perm1(msg1) 
        val hash2: BigInt = perm2(msg2)
        val hash3: BigInt = perm3(msg3)
        val hash4: BigInt = perm4(msg4)

        //val finalIn: String = hash1.toString + hash2.toString + hash3.toString + hash4.toString
        val finalIn: String = (hash1 ^ hash2 ^ hash3 ^ hash4).toString
        val final_perm = new PoseidonModel(p)
        val exp_out: BigInt = final_perm(Message(finalIn, 3))

        val m = MerkleParams(p, 4, 4)
        val mTree = new MerkleTreeModel(m)
        val model_out: BigInt = mTree(inSeq)
        assert(model_out == exp_out)
    }
}