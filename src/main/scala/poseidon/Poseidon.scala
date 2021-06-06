//TODO Abstract interface 

package poseidon

import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer
import scala.annotation.switch

object Poseidon {
    //val prime = PoseidonModel.prime.U 

    // States
    val idle :: loading :: firstRf :: rpRounds :: secondRf :: Nil = Enum(5)

    //val defParams = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5, matMulParallelism = 3)

    //Collapse Seq[UInt] to single UInt
    def collapseSeq(m : Seq[UInt]): UInt = m.tail.foldLeft(m.head) { case(a, b)  => Cat(a, b) }
}


class PoseidonIO(p: PoseidonParams) extends Bundle {
    val msg = Flipped(Decoupled(UInt((p.msgLen * 8).W)))
    val digest = Decoupled(UInt((p.hashLen * 8).W)) 
    override def cloneType = (new PoseidonIO(p)).asInstanceOf[this.type]
}

class Poseidon(p: PoseidonParams) extends Module {
    val io = IO(new PoseidonIO(p))
    
    //store input message in memory 
    val msg = Reg(UInt((p.msgLen*8).W))
    val stateVec = Reg(Vec(p.t, UInt((32*8).W)))
    val workVec = Reg(Vec(p.t, UInt((32*8).W)))
    val state = RegInit(Poseidon.idle)
    val roundConst = Reg(Vec(p.t*(p.Rf+p.Rp), UInt((32*8).W)))
    val MDSMtx = Reg(Vec(p.t, Vec(p.t, UInt((32*8).W))))

    val prime = Reg(UInt((32*8).W))
    if(p.bits==255){
      prime := PoseidonModel.prime255.U
    } 
    else{
      prime := PoseidonModel.prime254.U
    }
    io.msg.ready := (state === (Poseidon.idle))

    //Counter instantiation
    val (cycles, done) = Counter(0 until (p.Rf + p.Rp)*(3 + p.t*p.t/p.matMulParallelism), true.B, state === Poseidon.loading)
    
    val (roundCycles, roundDone) = Counter(0 until 3 + p.t*p.t/p.matMulParallelism, (state === Poseidon.firstRf) || (state === Poseidon.rpRounds) || (state === Poseidon.secondRf))
    val (fullCycles, fullRoundDone) = Counter(0 until p.Rf/2, roundDone && (state === Poseidon.firstRf || state === Poseidon.secondRf))
    val (partialCycles, partialRoundDone) = Counter(0 until p.Rp, roundDone && (state === Poseidon.rpRounds))

    val (index, indexDone) = Counter(0 until p.t*(p.Rf + p.Rp) by p.t, roundDone)
    
    //MatMul Counters
    val (kCycles, kDone) = Counter(0 until p.t, true.B, roundCycles === 1.U)
    val (rCycles, rDone) = Counter(0 until p.t, kDone, roundCycles === 1.U)
    
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
        if(p.t==3 & p.bits == 254){
          for(i <- 0 until p.t){
            for(j <- 0 until p.t){
              MDSMtx(i.U)(j.U) := PoseidonModel.fixedMDS254_3(i)(j).U
            }
          } 
          for(i <- 0 until p.t*(p.Rf+p.Rp)){
            roundConst(i) := PoseidonModel.fixedRoundConst254_3(i).U
          }
          }
          else if (p.t==5 & p.bits == 254){
            for(i <- 0 until p.t){
              for(j <- 0 until p.t){
                MDSMtx(i.U)(j.U) := PoseidonModel.fixedMDS254_5(i)(j).U
              }
            } 
            for(i <- 0 until p.t*(p.Rf+p.Rp)){
              roundConst(i) := PoseidonModel.fixedRoundConst254_5(i).U
            }
          }
          else if (p.t==3 & p.bits == 255){
            for(i <- 0 until p.t){
              for(j <- 0 until p.t){
                MDSMtx(i.U)(j.U) := PoseidonModel.fixedMDS255_3(i)(j).U
              }
            } 
            for(i <- 0 until p.t*(p.Rf+p.Rp)){
              roundConst(i) := PoseidonModel.fixedRoundConst255_3(i).U
            }
          }
          else if (p.t==5 & p.bits == 255){
            for(i <- 0 until p.t){
              for(j <- 0 until p.t){
                MDSMtx(i.U)(j.U) := PoseidonModel.fixedMDS255_5(i)(j).U
              }
            } 
            for(i <- 0 until p.t*(p.Rf+p.Rp)){
              roundConst(i) := PoseidonModel.fixedRoundConst255_5(i).U
            }
          }
        
        state := Poseidon.firstRf
      }

      is(Poseidon.firstRf){
        //First Rf/2 full rounds
        

        when(roundCycles === 0.U){

          //Add round constants
          for(i <- 0 until p.t){
            stateVec(i.U) := stateVec(i.U) + roundConst(i.U + index) % prime

          }

        }.elsewhen(roundCycles === 1.U){

          //S-box layer
          stateVec := stateVec.map(i => Seq.fill(p.alpha)(i).reduce(_*_) % prime)
          workVec := (0 until p.t).map(i => 0.U)
        }.elsewhen(roundCycles === 2.U + p.t.U * p.t.U/p.matMulParallelism.U){
          (0 until p.t).foreach(i => stateVec(i) := workVec(i) % prime)

        }
        .otherwise{
          //Mix: Matrix Multiplication Step

          switch(p.matMulParallelism.U){
            is(1.U){
              workVec(rCycles) := (workVec(rCycles) + (MDSMtx(rCycles)(kCycles) * stateVec(kCycles)) % prime) % prime
            }

            is(p.t.U){
              for(i <- 0 until p.matMulParallelism){
                workVec(i.U) := (workVec(i.U) + (MDSMtx(i.U)(kCycles) * stateVec(kCycles)) % prime) % prime
              }
            }
          }

        }
        
        when(fullRoundDone){
          state := Poseidon.rpRounds
        }.otherwise{
          state := Poseidon.firstRf
        }
      }

      is(Poseidon.rpRounds){
        //Partial rounds
        when(roundCycles === 0.U){

          //Add round constants
          for(i <- 0 until p.t){
            stateVec(i.U) := (stateVec(i.U) + roundConst(i.U + index)) % prime
          }
        }.elsewhen(roundCycles === 1.U){
          //S-box layer
          stateVec := stateVec.map(i => if( i == stateVec.head) Seq.fill(p.alpha)(i).reduce(_*_) % prime else i)
          workVec := (0 until p.t).map(i => 0.U)
        }.elsewhen(roundCycles === 2.U + p.t.U*p.t.U/p.matMulParallelism.U){
          (0 until p.t).foreach(i => stateVec(i) := workVec(i) % prime)
        }
        .otherwise{
          //Mix: Matrix Multiplication Step
          switch(p.matMulParallelism.U){
            is(1.U){
              workVec(rCycles) := (workVec(rCycles) + (MDSMtx(rCycles)(kCycles) * stateVec(kCycles)) % prime) % prime
            }

            is(p.t.U){
              for(i <- 0 until p.matMulParallelism){
                workVec(i.U) := (workVec(i.U) + (MDSMtx(i.U)(kCycles) * stateVec(kCycles)) % prime) % prime
              }
            }
          }
        }

        when(partialRoundDone){
          state := Poseidon.secondRf
        }.otherwise{
          state := Poseidon.rpRounds
        }

      }

      is(Poseidon.secondRf){

        //Last Rf/2 full rounds
        when(roundCycles === 0.U){
          //Add round constants
          for(i <- 0 until p.t){
            stateVec(i.U) := (stateVec(i.U) + roundConst(i.U + index)) % prime
          }
        }.elsewhen(roundCycles === 1.U){
          //S-box layer

          stateVec := stateVec.map(i => Seq.fill(p.alpha)(i).reduce(_*_) % prime)
          workVec := (0 until p.t).map(i => 0.U)
        }.elsewhen(roundCycles === 2.U + p.t.U*p.t.U/p.matMulParallelism.U){
          (0 until p.t).foreach(i => stateVec(i) := workVec(i) % prime)
        }
        .otherwise{
          //Mix: Matrix Multiplication Step
          switch(p.matMulParallelism.U){
            is(1.U){
              workVec(rCycles) := (workVec(rCycles) + (MDSMtx(rCycles)(kCycles) * stateVec(kCycles)) % prime) % prime
            }

            is(p.t.U){
              for(i <- 0 until p.matMulParallelism){
                workVec(i.U) := (workVec(i.U) + (MDSMtx(i.U)(kCycles) * stateVec(kCycles)) % prime) % prime
              }
            }
          }
        }

        when(fullRoundDone){
          state := Poseidon.idle
        }.otherwise{
          state := Poseidon.secondRf
        }
      }
    }
    io.digest.valid := RegNext(done)
    io.digest.bits := Poseidon.collapseSeq(stateVec)

}