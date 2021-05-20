package poseidon

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._

 class PoseidonHashTester extends FreeSpec with ChiselScalatestTester {
    def testPoseidon(p: PoseidonParams, m: Message): Boolean = {
        val hashModel = new PoseidonModel(p)
        val expHash = hashModel(m)
        test(new Poseidon(p)).withAnnotations(Seq(WriteVcdAnnotation)) { c => 
            c.io.msg.bits.poke(m.U)
            c.io.msg.valid.poke(true.B)
            c.io.msgBytes.poke(m.length.U)
            c.clock.step(1)
            c.io.msg.valid.poke(false.B)
            c.clock.step(p.numRounds) // proceed through half of the rounds
            // attempt to enter another valid msg that should be ignored 
            c.io.msg.bits.poke(12345678.U)
            c.io.msg.valid.poke(true.B)
            c.clock.step(p.numRounds) // 2nd half of rounds (remember 4-col OR 4-diag per cycle)
            c.io.digest.valid.expect(true.B)

            // Process the output in Scala land and compare to the expected
            val got = c.io.digest.bits.peek().litValue
            val gotWords = (0 until 8).map(i => (got >> i*p.wordSize) & p.MASK)
            val gotHash = Blake2Model.wordsToMessage(gotWords, p.wordSize)
            println(s"hash('${m.str}') = ${gotHash.str}")
            assert (gotHash == expHash)
        }
        true
    }

    "Hardware Poseidon module should compute correct hash of ''" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("1", p.t)
        testBlake2(p, m) 
    }

 }
