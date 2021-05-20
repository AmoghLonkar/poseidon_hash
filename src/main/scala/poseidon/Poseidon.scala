package hw5

import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer

object Poseidon {
    // Initialization Vectors - "first 64 bits of the fractional parts of the positive square roots of the first eight prime numbers")
    val fixedRoundConst = PoseidonModel.fixedRoundConst.map(_.U) 
    val prime = PoseidonModel.prime.U 
    val fixedMDS = PoseidonModel.fixedMDS.map(_.map(_.U))



    // pretty print chisel to match test vector
    def pHex(m: Seq[UInt], width: Int=64) = {
      if (m.length == 16) {
        for (i <- 0 until 16) {
          val nl = if (Seq(2, 6, 10, 14).contains(i)) "\n" else ""
          printf(p"${Hexadecimal(m(i)(width - 1, 0))} $nl")
        }
        printf("\n")
      }
    }
        
    // Concats a Seq[UInt] to a single UInt 
    def collapseSeq(m : Seq[UInt]): UInt = m.tail.foldLeft(m.head) { case(a, b)  => Cat(b, a) }

    val defParams = PoseidonParams(r = 64, c = 64, Rf = 8, Rp = 57, alpha = 5)

}


class PoseidonIO(p: PoseidonParams) extends Bundle {
    //require (p.msgLen == 128, "FUTURE: accept msgLen != 128B")
    val msg = Flipped(Decoupled(UInt((p.msgLen * 8).W)))
    //val msgBytes = Input(UInt(128.W)) // 128-bit t
    val digest = Decoupled(UInt((p.hashLen * 8).W)) 
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
    val workVec = Reg(Vec(p.t, UInt((32*8).W)))
    //val msg_array : Seq[UInt] = for (i <- 0 to p.t by 1) yield ((io.msg.bits((i*32), i+1*32)))
    var ready = RegInit(1.B)
    io.msg.ready:=ready
    io.digest.valid := 0.B
    io.digest.bits := 0.U
    
    for(i<- 0 until p.t){
        workVec(i.U) := (io.msg.bits((i+1)*256-1), i*256)
        printf("input %d: %x \n",i.U, workVec(i))
    }

    // for i in range(0, t):
    //         state_words[i] = state_words[i] + round_constants_field[round_constants_counter]
    //         round_constants_counter += 1


    // for i in range(0, t):
    //     state_words[i] = (state_words[i])^5

    // state_words = list(MDS_matrix_field * vector(state_words))
}