package poseidon

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._

import scala.collection.mutable.ArrayBuffer

class PoseidonModelTester extends FreeSpec with ChiselScalatestTester {
    // "Software full round should work" in {
    //     val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))

    //     val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
    //     val raw_out = Seq("282632456682227911414206241016488835686554689305466568982077256658250712558", "11917093825321412328192341813986995477498953231172500502534701133527538595380", "16178523585147542488843960293009279670169469496632225926281827801976870407225")
    //     val exp_out = ArrayBuffer[BigInt]()
    //     (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i)))
    //     //assert(PoseidonModel.roundFunction(p, v, true, 0) == exp_out)
    //     assert(PoseidonModel.roundFunction(p, v, true) == exp_out)

    // }
    // "Software half round should work" in {
    //     val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
    //     val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
    //     val raw_out = Seq("14965134945793732865350679931124011880240139321801211710425778720571148366276", "3868204542908165419123931668177036829299999539168523914519394764625024544340", "3235367473085521110766631874957383689991448664301590931405940712777711390853")
    //     val exp_out = ArrayBuffer[BigInt]()
    //     (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i)))
    //     //assert(PoseidonModel.roundFunction(p, v, false, 0) == exp_out)
    //     assert(PoseidonModel.roundFunction(p, v, false) == exp_out)

    // }

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