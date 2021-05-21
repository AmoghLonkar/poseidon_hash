package hw5

import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer
import scala.annotation.switch

object Poseidon {
    // Converting given constants to UInt
    val fixedRoundConst = PoseidonModel.fixedRoundConst.map(_.U) 
    val prime = PoseidonModel.prime.U 
    val fixedMDS = PoseidonModel.fixedMDS.map(_.map(_.U))

    // States
    val idle :: loading :: firstRf :: Rp :: secondRf :: Nil = Enum(5)

    val defParams = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)

    //Collapse Seq[UInt] to single UInt
    def collapseSeq(m : Seq[UInt]): UInt = m.tail.foldLeft(m.head) { case(a, b)  => Cat(b, a) }
}


class PoseidonIO(p: PoseidonParams) extends Bundle {
    //require (p.msgLen == 128, "FUTURE: accept msgLen != 128B")
    val msg = Flipped(Decoupled(SInt((p.msgLen * 8).W)))
    //val msgBytes = Input(UInt(128.W)) // 128-bit t
    val digest = Decoupled(SInt((p.hashLen * 8).W)) 
    override def cloneType = (new PoseidonIO(p)).asInstanceOf[this.type]
}


/**
  * When a valid msg arrives, the next cycle ready goes low and the hash will begin.
  * 
  *
  * @param p
  */
class Poseidon(p: PoseidonParams=Poseidon.defParams) extends Module {
    val io = IO(new PoseidonIO(p))
    // BEGIN SOLUTION
    //store input message in memory 
    val msg = Reg(UInt((p.msgLen*8).W))
    val stateVec = Reg(Vec(p.t, UInt((32*8).W)))
    val state = RegInit(Poseidon.idle)
    io.msg.ready := state == (Poseidon.idle)

    //Counter instantiation
    val (cycles, done) = Counter(0 until (p.Rf + p.Rp) + ??? , true.B, io.msg.valid && !onGoing)
    val (fullCycles, fullRoundDone) = Counter(0 until p.Rf/2, (state === Poseidon.firstRf) || (state === Poseidon.secondRf))
    val (partialCycles, partialRoundDone) = Counter(0 until p.Rp, state === Poseidon.Rp)

    switch(state){
      is(Poseidon.idle){
        when(io.msg.valid && io.msg.ready){
          state := Poseidon.loading
        }.otherwise{
          state := Poseidon.idle
        }
      }

      is(Poseidon.loading){
        //Single cycle transfer for now  
        msg := io.msg.bits
        stateVec := VecInit((0 until p.t).map(i => io.msg.bits((i+1)*256 - 1, i*256)))

          state := Poseidon.firstRf
      }
      
      is(Poseidon.firstRf){

        when(fullRoundDone){
          state := Poseidon.Rp
        }.otherwise{
          state := Poseidon.firstRf
        }
      }

      is(Poseidon.Rp){
        
        when(partialRoundDone){
          state := Poseidon.secondRf
        }.otherwise{
          state := Poseidon.Rp
        }
      }

      is(Poseidon.secondRf){
        
        when(fullRoundDone){
          state := Poseidon.idle
        }.otherwise{
          state := Poseidon.secondRf
        }
      }
    }
    
    
    
    }.otherwise{
      //Perform permutation

      //First half full rounds

      //All Partial Rounds

      //Second half full rounds

    }

    io.digest.ready := RegNext(done)
    io.digest.bits := collapseSeq(stateVec)
}