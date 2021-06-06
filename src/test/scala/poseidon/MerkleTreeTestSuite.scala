package poseidon

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._

import scala.math._

class MerkleTreeTester extends FreeSpec with ChiselScalatestTester {

    def testMerkleTree(m: MerkleParams, inSeq: Seq[Message]): Boolean = {
        val treeModel = new MerkleTreeModel(m)
        val treeHeight = (log10(m.numNodes)/log10(m.treeRadix)).ceil.toInt
        val exp_out = treeModel(inSeq)
        test(new MerkleTree(m)).withAnnotations(Seq(WriteVcdAnnotation)) { c => 
            c.io.msg.valid.poke(true.B)
            c.io.msg.ready.expect(true.B)
            c.io.digest.valid.expect(false.B)
            c.clock.step()
            if(m.numInputs == 2){
                c.io.msg.bits.poke(inSeq(0).string2BigInt.U)
                c.clock.step()
                c.io.msg.bits.poke(inSeq(1).string2BigInt.U)
            }
            else if(m.numInputs == 4){
                c.io.msg.bits.poke(inSeq(0).string2BigInt.U)
                c.clock.step()
                c.io.msg.bits.poke(inSeq(1).string2BigInt.U)
                c.clock.step()
                c.io.msg.bits.poke(inSeq(2).string2BigInt.U)
                c.clock.step()
                c.io.msg.bits.poke(inSeq(3).string2BigInt.U)
            }
            c.clock.step((treeHeight)*((m.p.Rf + m.p.Rp)*(3 + m.p.t*m.p.t/m.p.matMulParallelism)))
            c.clock.step()
            c.io.digest.valid.expect(true.B)
            c.clock.step(4)
            c.io.digest.bits.expect(exp_out.U)
        }
        true
    }

    "Hardware MerkleTree root should store the correct value in simple 2-to-1 tree" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, matMulParallelism = 3)
        val m = MerkleParams(p, 2, 2)

        val msg1 = Message("a", 3)
        val msg2 = Message("b", 3)
        val inSeq = Seq(msg1, msg2)

        testMerkleTree(m, inSeq)
    }

    "Hardware MerkleTree root should store the correct value in simple 4-to-1 tree" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, matMulParallelism = 3)
        val m = MerkleParams(p, 4, 4)

        val msg1 = Message("a", 3)
        val msg2 = Message("b", 3)
        val msg3 = Message("c", 3)
        val msg4 = Message("d", 3)
        val inSeq = Seq(msg1, msg2, msg3, msg4)

        testMerkleTree(m, inSeq)
    }

    "Hardware MerkleTree root should store the correct value in simple 2-to-1 tree (bits = 255)" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, bits = 255, matMulParallelism = 3)
        val m = MerkleParams(p, 2, 2)
        val msg1 = Message("a", 3)
        val msg2 = Message("b", 3)
        val inSeq = Seq(msg1, msg2)

        testMerkleTree(m, inSeq)
    }

    "Hardware MerkleTree root should store the correct value in simple 4-to-1 tree (bits = 255)" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, bits = 255, matMulParallelism = 3)
        val m = MerkleParams(p, 4, 4)

        val msg1 = Message("a", 3)
        val msg2 = Message("b", 3)
        val msg3 = Message("c", 3)
        val msg4 = Message("d", 3)
        val inSeq = Seq(msg1, msg2, msg3, msg4)

        testMerkleTree(m, inSeq)
    }

}