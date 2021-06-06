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

        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, t=3)
        val raw_out = Seq("8911802574348315987449836474871354036259397526939586959061211560439926436096", "985952325209333242621206154741933566805100493600790329107350091977382796129", "5624942857022694920016589052460625832985228527533686355747501579407992302578")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i)))
        assert(PoseidonModel.roundFunction(p, v, true, 0) == exp_out)
    }

    "Software half round should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, t=3)
        val raw_out = Seq("8922116320408198701234205881974946777532428279772296419481941921645749524661", "11958583305522522826214344438521253929156446831200313855904403507320628382882", "6647132360235631115914024297955397799760950858886512568603794167233508926731")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i)))
        assert(PoseidonModel.roundFunction(p, v, false, 0) == exp_out)
    }

    "Software Permutation 254 t3 should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, t=3)
        val raw_out = Seq("115cc0f5e7d690413df64c6b9662e9cf2a3617f2743245519e19607a4417189a", "0fca49b798923ab0239de1c9e7a4a9a2210312b6a2f616d18b5a87f9b628ae29", "0e7ae82e40091e63cbd4f16a6d16310b3729d4b6e138fcf54110e2867045a30c")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i),16))
        assert(PoseidonModel.Permutation(p, v) == exp_out)
    }

    "Software Permutation 254 t5 should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2),BigInt(3),BigInt(4))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 60, alpha = 5, t=5)
        val raw_out = Seq("299c867db6c1fdd79dcefa40e4510b9837e60ebb1ce0663dbaa525df65250465", "1148aaef609aa338b27dafd89bb98862d8bb2b429aceac47d86206154ffe053d", "24febb87fed7462e23f6665ff9a0111f4044c38ee1672c1ac6b0637d34f24907","0eb08f6d809668a981c186beaf6110060707059576406b248e5d9cf6e78b3d3e","07748bc6877c9b82c8b98666ee9d0626ec7f5be4205f79ee8528ef1c4a376fc7")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i),16))
        assert(PoseidonModel.Permutation(p, v) == exp_out)
    }
    "Software Permutation 255 t3 should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, t=3, prime=255)
        val raw_out = Seq("28ce19420fc246a05553ad1e8c98f5c9d67166be2c18e9e4cb4b4e317dd2a78a", "51f3e312c95343a896cfd8945ea82ba956c1118ce9b9859b6ea56637b4b1ddc4", "3b2b69139b235626a0bfb56c9527ae66a7bf486ad8c11c14d1da0c69bbe0f79a")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i),16))
        assert(PoseidonModel.Permutation(p, v) == exp_out)
    }
        "Software Permutation 255 t5 should work" in {
        val v = ArrayBuffer(BigInt(0),BigInt(1),BigInt(2),BigInt(3),BigInt(4))
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 60, alpha = 5, t=5, prime=255)
        val raw_out = Seq("2a918b9c9f9bd7bb509331c81e297b5707f6fc7393dcee1b13901a0b22202e18", "65ebf8671739eeb11fb217f2d5c5bf4a0c3f210e3f3cd3b08b5db75675d797f7", "2cc176fc26bc70737a696a9dfd1b636ce360ee76926d182390cdb7459cf585ce","4dc4e29d283afd2a491fe6aef122b9a968e74eff05341f3cc23fda1781dcb566","03ff622da276830b9451b88b85e6184fd6ae15c8ab3ee25a5667be8592cce3b1")
        val exp_out = ArrayBuffer[BigInt]()
        (0 until p.t).foreach(i => exp_out += BigInt(raw_out(i),16))
        assert(PoseidonModel.Permutation(p, v) == exp_out)
    }


    "Software PoseidonModel should compute correct hash of ''" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("", t = p.t)

        val hash = new PoseidonModel(p)
        val enc_val: BigInt = hash(m)

        val exp_out = "197688335131797370695180580151217351502119230164689865811475600117404037432827440483317360774210043247610027392616560824454747064500449562359306238995993386154528235824338843606202069182854679258815511457002223009305442195885659264"
        assert(enc_val.toString == exp_out)
    }

    "Software PoseidonModel should compute correct hash of 'abc'" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("abc", t = p.t)

        val hash = new PoseidonModel(p)
        val enc_val: BigInt = hash(m)

        val exp_out = "288472530165565618859032654869442734644388118676745616022880934313018061647186325057982985611162619606922082564842439795167759984159416029837695261752759370132598963414891667630524813778002142387526219195492313182143215684994033140"
        assert(enc_val.toString == exp_out)
    }

    "Software PoseidonModel should compute correct hash of 'Chisel is too much fun!'" in {
        val p = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)
        val m = Message("Chisel is too much fun!", t = p.t)

        val hash = new PoseidonModel(p)
        val enc_val: BigInt = hash(m)

        val exp_out = "262222900148737331592691600326130556942093573874115640764967070768086322480649097475397454884094760502462283064684920903881564935587717050950305899450791985060878008763548962549070477916526735601414865833678544320809602941575078725"
        assert(enc_val.toString == exp_out)
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