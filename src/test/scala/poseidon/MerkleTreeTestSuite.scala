package poseidon

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._

class MerkleTreeTester extends FreeSpec with ChiselScalatestTester {

    def testMerkleTree(m: MerkleParams, inSeq: Seq[Message]): Boolean = {
        val treeModel = new MerkleTreeModel(m)
        val exp_out = treeModel(inSeq)
        test(new MerkleTree(m)).withAnnotations(Seq(WriteVcdAnnotation)) { c => 
            c.io.msg.valid.poke(true.B)
            c.io.msg.ready.expect(true.B)
            c.io.digest.valid.expect(false.B)
            c.clock.step()
            c.io.msg.bits.poke(inSeq(0).string2BigInt.U)
            c.clock.step()
            c.io.msg.bits.poke(inSeq(1).string2BigInt.U)
            c.clock.step()
        }
        true
    }

    "Hardware MerkleTree root should store the correct value in simple 2-to-1 tree" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, parallelism = 3)
        val m = MerkleParams(p, 2, 2)

        val msg1 = Message("a", 3)
        val msg2 = Message("b", 3)
        val inSeq = Seq(msg1, msg2)

        testMerkleTree(m, inSeq)
    }
}