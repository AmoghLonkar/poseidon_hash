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

    "Software Permutation should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val raw_out = Seq("115cc0f5e7d690413df64c6b9662e9cf2a3617f2743245519e19607a4417189a", "0fca49b798923ab0239de1c9e7a4a9a2210312b6a2f616d18b5a87f9b628ae29", "0e7ae82e40091e63cbd4f16a6d16310b3729d4b6e138fcf54110e2867045a30c")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i),16))
        assert(PoseidonModel.Permutation(p, v) == exp_out)
    }


    "Software PoseidonModel should compute correct hash of ''" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("", t = p.t)

        val hash = new PoseidonModel(p)
        val enc_val: ArrayBuffer[BigInt] = hash(m)

        val exp_out = ArrayBuffer(BigInt("2098f5fb9e239eab3ceac3f27b81e481dc3124d55ffed523a839ee8446b64864", 16), BigInt("13a545a13f1d91dddb87f46679dfaec0900ce24791a924bee7fa4d69a9569d85", 16), BigInt("06be479e5fcd717c6c21b32f108033bf1da6cf4d8e3e8c48042c475e0b121480", 16))
        assert(enc_val == exp_out)
    }

    "Software PoseidonModel should compute correct hash of 'abc'" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("abc", t = p.t)

        val hash = new PoseidonModel(p)
        val enc_val: ArrayBuffer[BigInt] = hash(m)

        val exp_out = ArrayBuffer(BigInt("2f91353f9fbde8590825cb6b0c0396336e1d2bbe1a8f444e68e5aa5dff59f7fd", 16), BigInt("24e7ef2e211d4f4f7bfeb6521f06308a433a2f5ea4c39eef6ee66d39d235d72e", 16), BigInt("1fee6a6576e68646d304103398c9eb0d66115ca24fe2eefb282abdc5350d65f4", 16))
        assert(enc_val == exp_out)
    }

    "Software PoseidonModel should compute correct hash of 'Chisel is too much fun!'" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("Chisel is too much fun!", t = p.t)

        val hash = new PoseidonModel(p)
        val enc_val: ArrayBuffer[BigInt] = hash(m)

        val exp_out = ArrayBuffer(BigInt("06be10bf31fce4c8a8d686f3f48b7dc16f378df97a093f13f7ebfa9d8796e73c", 16), BigInt("0ff2e5684842d277b7adbaf9b07af9764e0071262b4aebcd01b87f0352a64435", 16), BigInt("0a868ecb2af41ddfeb272dce9ec103f25fe8a48ac54e659a935b9658227c82cd", 16))
        assert(enc_val == exp_out)
    }
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
        val stateVec = msg.string2Chunks()
        val exp_out = BigInt("63686973656c", 16)
        assert(msg.chunks2BigInt(stateVec) == exp_out)
    }
}