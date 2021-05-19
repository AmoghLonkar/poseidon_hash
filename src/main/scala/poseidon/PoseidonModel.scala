package poseidon

import chisel3._

object PoseidonModel{
    

}

case class PoseidonParams(field: Seq[BigInt], r: Int, c: Int, Rf: Int, Rp: Int){
    val m = r + c
    require(Rf % 2 == 0)
    
    val num_rounds = Rf + Rp
    val out_size = c
    require(out_size < r)

    val round_const = ???

    val mds_mtx: Seq[Seq[BigInt]] = ???
}

object MatMul {
  type Matrix = Seq[Seq[Int]]

  def apply(a: Matrix, b: Matrix): Matrix = {
        // BEGIN SOLUTION
    
    def grabCol(m: Seq[Seq[Int]], i: Int) = m map { _(i) }

    def dotP(a: Seq[Int], b: Seq[Int]) = a.zip(b).map{ case (x,y) => x * y}.sum
    
    def matMul(a: Seq[Seq[Int]], b: Seq[Seq[Int]]) = Seq.tabulate(a.size, b.head.size){
     case (i,j) => dotP(a(i), grabCol(b,j))
    }
    matMul(a, b)
  }

class PoseidonModel(p: PoseidonParams){
    
    def Permutation(values: Seq[BigInt]){
        require(values.length == p.m)
        var index = 0

        for(i <- 0 until p.Rf / 2){
            values = roundFunction(values, true, index)
            index += 1
        }

        for(i <- 0 until p.Rp){
            values = roundFunction(values, false, index)
            index += 1
        }

        for(i <- 0 until p.Rf/2){
            values = roundFunction(values, true, index)
            index += 1
        }
        assert(index == p.num_rounds)
        values
    }

    def roundFunction(values: Seq[BigInt], roundType: Boolean, index: int){
        values += p.round_const[index]

        if(roundType){
            values = (0 until values.length).map(i => Seq.fill(3)(i).reduce(_*_))
        }
        else{
            values = (0 until values.length) map (i => if(i == l.length - 1) Seq.fill(3)(i).reduce(_*_) else values(i))
        }

        //Mix
        values = MatMul(values, p.mds_mtx)
    }

    /*
    def HashOut(){

    }*/
}