package poseidon

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._

import scala.collection.mutable.ArrayBuffer

class PoseidonModelTester extends FreeSpec with ChiselScalatestTester {
    "Software full round should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))

        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val raw_out = Seq("8911802574348315987449836474871354036259397526939586959061211560439926436096", "985952325209333242621206154741933566805100493600790329107350091977382796129", "5624942857022694920016589052460625832985228527533686355747501579407992302578")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i)))
        assert(PoseidonModel.roundFunction(p, v, true, 0) == exp_out)

    }
    "Software half round should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val raw_out = Seq("8922116320408198701234205881974946777532428279772296419481941921645749524661", "11958583305522522826214344438521253929156446831200313855904403507320628382882", "6647132360235631115914024297955397799760950858886512568603794167233508926731")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i)))
        assert(PoseidonModel.roundFunction(p, v, false, 0) == exp_out)

    }

    "Software Poseidon254_3 should compute correct hash of 'Seq(0,1,2)'" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val raw_out = Seq("115cc0f5e7d690413df64c6b9662e9cf2a3617f2743245519e19607a4417189a", "0fca49b798923ab0239de1c9e7a4a9a2210312b6a2f616d18b5a87f9b628ae29", "0e7ae82e40091e63cbd4f16a6d16310b3729d4b6e138fcf54110e2867045a30c")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i),16))
        assert(PoseidonModel.Permutation(p, v) == exp_out)
    }

    // "Software Blake2Model should compute correct hash of ''" in {
    //     
    // }

}

class MessageTester extends FreeSpec with ChiselScalatestTester {
    "Message should convert input string into stateVec of 3, 32B chunks" in {
        val msg = Message("chisel", 3)

        val exp_out = ArrayBuffer(BigInt(0), BigInt(0), BigInt("63686973656c", 16))
        assert(msg.string2Chunks() == exp_out)
    }
    
    "Message should convert input string into BigInt" in {
        val msg = Message("chisel", 3)

        val exp_out = BigInt("63686973656c", 16)
        assert(msg.string2BigInt() == exp_out)
    }

    "Message should convert final, hashed stateVec of 3, 32B chunks into BigInt" in {
        val msg = Message("chisel", 3)
        //val stateVec = msg.string2Chunks()
        val stateVec = ArrayBuffer(BigInt(0), BigInt(1), BigInt(2))
        val exp_out = BigInt("63686973656c", 16)
        assert(msg.chunks2BigInt(stateVec) == exp_out)
    }
}