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
            c.io.msg.valid.poke(true.B)
            c.io.msg.ready.expect(true.B)
            c.io.digest.valid.expect(false.B)
            c.clock.step()
            c.io.msg.bits.poke(m.string2BigInt.U)
            c.clock.step()
            c.io.msg.valid.poke(false.B)
            c.clock.step((p.Rf + p.Rp)*(3 + p.t*p.t/p.parallelism))
            c.io.digest.valid.expect(true.B)
            c.io.digest.bits.expect(expHash.U)
        }
        true
    }

    "Hardware Poseidon module should compute correct hash of ''" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("", p.t)
        testPoseidon(p, m) 
    }

    "Hardware Poseidon module should compute correct hash of 'abc'" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("abc", p.t)
        testPoseidon(p, m) 
    }

    "Hardware Poseidon module should compute correct hash of 'Chisel is too much fun!'" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("Chisel is too much fun!", p.t)
        testPoseidon(p, m) 
    }

    "Hardware Poseidon module should compute correct hash of '' using parallel MatMul" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, parallelism = 3)
        val m = Message("", p.t)
        testPoseidon(p, m) 
    }

    "Hardware Poseidon module should compute correct hash of 'abc' using parallel MatMul" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, parallelism = 3)
        val m = Message("abc", p.t)
        testPoseidon(p, m) 
    }

    "Hardware Poseidon module should compute correct hash of 'Chisel is too much fun!' using parallel MatMul" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, parallelism = 3)
        val m = Message("Chisel is too much fun!", p.t)
        testPoseidon(p, m) 
    }
 }
