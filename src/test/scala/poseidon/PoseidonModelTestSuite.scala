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
        val raw_out = Seq("282632456682227911414206241016488835686554689305466568982077256658250712558", "11917093825321412328192341813986995477498953231172500502534701133527538595380", "16178523585147542488843960293009279670169469496632225926281827801976870407225")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i)))
        assert(PoseidonModel.roundFunction(p, v, true, 0) == exp_out)

    }
    "Software half round should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val raw_out = Seq("14965134945793732865350679931124011880240139321801211710425778720571148366276", "3868204542908165419123931668177036829299999539168523914519394764625024544340", "3235367473085521110766631874957383689991448664301590931405940712777711390853")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i)))
        assert(PoseidonModel.roundFunction(p, v, false, 0) == exp_out)

    }

    // "Software Blake2Model should compute correct hash of 'abc'" in {
    //     // val p = Blake2Params(hashLen = 64, msgLen = 64)
    //     // val m = Message("abc", wordSize = p.wordSize)
    //     // m.printWords
    //     // val hash = new Blake2Model(p)

    //     // val out: Message = hash(m)

    //     // val expStr = "ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d17d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923"
    //     // println(f"got: ${out.str}, exp: $expStr")
    //     // assert(out.str == expStr)
    // }

    // "Software Blake2Model should compute correct hash of ''" in {
    //     
    // }


}