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
        val raw_out = Seq("1434672155438326909468395403595442108100131599008064476955240820749368388332", "4833344523483475792495452519536041633104809104825374711308215753935826116910", "1220913282961632239274840494584456300408222746440926547802646300125394075962")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i), 16))
        assert(PoseidonModel.roundFunction(p, v, true, 0) == exp_out)

    }
    "Software half round should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val raw_out = Seq("21038416477568674046849954456272533174388763670098315194144749698091882799577", "19568226743792614801321798830704698149002855748864219953241937962343579857238", "10804600181627836747048859459573044908538498894105223567676355450893040239813")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i), 16))
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