package poseidon

import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer
import scala.annotation.switch

object Poseidon {
    val prime = PoseidonModel.prime.U 

    // States
    val idle :: loading :: firstRf :: rpRounds :: secondRf :: Nil = Enum(5)

    val defParams = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)

    //Collapse Seq[UInt] to single UInt
    def collapseSeq(m : Seq[UInt]): UInt = m.tail.foldLeft(m.head) { case(a, b)  => Cat(b, a) }
}


class PoseidonIO(p: PoseidonParams) extends Bundle {
    //require (p.msgLen == 128, "FUTURE: accept msgLen != 128B")
    val msg = Flipped(Decoupled(UInt((p.msgLen * 8).W)))
    //val msgBytes = Input(UInt(128.W)) // 128-bit t
    val digest = Decoupled(UInt((p.hashLen * 8).W)) 
    override def cloneType = (new PoseidonIO(p)).asInstanceOf[this.type]
}

class Poseidon(p: PoseidonParams=Poseidon.defParams) extends Module {
    val io = IO(new PoseidonIO(p))
    // BEGIN SOLUTION
    //store input message in memory 
    val msg = Reg(UInt((p.msgLen*8).W))
    val stateVec = Reg(Vec(p.t, UInt((32*8).W)))
    val workVec = Reg(Vec(p.t, UInt((32*8).W)))
    val state = RegInit(Poseidon.idle)
    val roundConst = Reg(Vec(p.t*(p.Rf+p.Rp), UInt((32*8).W)))
    val MDSMtx = Reg(Vec(p.t, Vec(p.t, UInt((32*8).W))))
    
    io.msg.ready := (state === (Poseidon.idle))

    //Counter instantiation
    val (cycles, done) = Counter(0 until (p.Rf + p.Rp)*(3 + p.t*p.t), true.B, state === Poseidon.loading)
    
    val (roundCycles, roundDone) = Counter(0 until 3 + p.t*p.t, (state === Poseidon.firstRf) || (state === Poseidon.rpRounds) || (state === Poseidon.secondRf))
    val (fullCycles, fullRoundDone) = Counter(0 until p.Rf/2, roundDone && (state === Poseidon.firstRf || state === Poseidon.secondRf))
    val (partialCycles, partialRoundDone) = Counter(0 until p.Rp, roundDone && (state === Poseidon.rpRounds))

    val (index, indexDone) = Counter(0 until p.t*(p.Rf + p.Rp) by p.t, roundDone)
    
    //MatMul Counters
    val (kCycles, kDone) = Counter(0 until p.t, true.B, roundCycles === 2.U)
    val (rCycles, rDone) = Counter(0 until p.t, kDone)
    
    val (tCycles, tDone) = Counter(0 until p.t*p.t, (state === Poseidon.firstRf) || (state === Poseidon.rpRounds) || (state === Poseidon.secondRf), roundCycles === 2.U)
    
    
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
        stateVec := VecInit((0 until p.t).map(i => io.msg.bits((i+1)*256 - 1, i*256)).reverse)
        for(i <- 0 until p.t){
          for(j <- 0 until p.t){
            MDSMtx(i.U)(j.U) := PoseidonModel.fixedMDS(i)(j).U
          }
        }

        for(i <- 0 until p.t*(p.Rf+p.Rp)){
          roundConst(i) := PoseidonModel.fixedRoundConst(i).U
        }

        state := Poseidon.firstRf
      }
      
      is(Poseidon.firstRf){
        //First Rf/2 full rounds
        when(fullCycles === 0.U){
          //Add round constants
          for(i <- 0 until p.t){
            stateVec(i.U) := stateVec(i.U) + roundConst(i.U + index)
          }
        }.elsewhen(fullCycles === 1.U){
          //S-box layer
          stateVec := stateVec.map(i => Seq.fill(p.alpha)(i).reduce(_*_) % Poseidon.prime)
          workVec := stateVec.map(i => Seq.fill(p.alpha)(i).reduce(_*_) % Poseidon.prime)
        }.otherwise{
          //Mix: Matrix Multiplication Step
          workVec(rCycles) := workVec(rCycles) + (MDSMtx(rCycles)(kCycles) * stateVec(kCycles))
        }
        
        when(fullRoundDone){
          (0 until p.t).foreach(i => stateVec(i) := stateVec(i) % Poseidon.prime)
          state := Poseidon.rpRounds
        }.otherwise{
          state := Poseidon.firstRf
        }
      }

      is(Poseidon.rpRounds){
        //Partial rounds
        when(fullCycles === 0.U){
          //Add round constants
          for(i <- 0 until p.t){
            stateVec(i.U) := stateVec(i.U) + roundConst(i.U + index)
          }
        }.elsewhen(fullCycles === 1.U){
          //S-box layer
          stateVec := stateVec.map(i => if( i == stateVec.head) Seq.fill(p.alpha)(i).reduce(_*_) % Poseidon.prime else i)
          workVec := stateVec.map(i => if( i == workVec.head) Seq.fill(p.alpha)(i).reduce(_*_) % Poseidon.prime else i)
        }.otherwise{
          //Mix: Matrix Multiplication Step
          workVec(rCycles) := workVec(rCycles) + (MDSMtx(rCycles)(kCycles) * stateVec(kCycles))
        }

        when(partialRoundDone){
          (0 until p.t).foreach(i => stateVec(i) := stateVec(i) % Poseidon.prime)
          state := Poseidon.secondRf
        }.otherwise{
          state := Poseidon.rpRounds
        }
      }

      is(Poseidon.secondRf){
        
        //Last Rf/2 full rounds
        when(fullCycles === 0.U){
          //Add round constants
          for(i <- 0 until p.t){
            stateVec(i.U) := stateVec(i.U) + roundConst(i.U + index)
          }
        }.elsewhen(fullCycles === 1.U){
          //S-box layer
          stateVec := stateVec.map(i => Seq.fill(p.alpha)(i).reduce(_*_) % Poseidon.prime)
          workVec := stateVec.map(i => Seq.fill(p.alpha)(i).reduce(_*_) % Poseidon.prime)
        }.otherwise{
          //Mix: Matrix Multiplication Step
          workVec(rCycles) := workVec(rCycles) + (MDSMtx(rCycles)(kCycles) * stateVec(kCycles))
        }

        when(fullRoundDone){
          (0 until p.t).foreach(i => stateVec(i) := workVec(i) % Poseidon.prime)
          state := Poseidon.idle
        }.otherwise{
          state := Poseidon.secondRf
        }
      }
    }
    
    io.digest.valid := RegNext(done)
    io.digest.bits := Poseidon.collapseSeq(stateVec)
}