package poseidon

import chisel3._ 
import scala.math._
import scala.collection.mutable.ArrayBuffer

case class MerkleParams(p: PoseidonParams, numChild: Int = 2){

}


case class Node(m: MerkleParams, data: Message, children: Seq[Int]) {
    val hashFn = new PoseidonModel(m.p)
    val hash = hashFn(data)
}

object MerkleTreeModel{
   
    def log2ceil(in: Int): Int = { (log10(in)/log10(2)).ceil.toInt }

    def BuildTree(m: MerkleParams, inputs: Seq[Message]): ArrayBuffer[Node] = {
        //Initialize tree
        val tree = new ArrayBuffer[Node]()
        val numNodes: Int = (0 to log2ceil(inputs.size)).map( i => (inputs.size/pow(m.numChild, i)).ceil).sum.toInt
        (0 until numNodes).foreach(i => if(i < numNodes - inputs.size) { tree += Node(m, Message("", m.p.t), Seq.tabulate(m.numChild)(j => m.numChild*i + j+1)) } else { tree += Node(m, inputs(i+1 - inputs.size), Seq.fill(1)(i)) })

        //Iterate up the tree to get final hash
        for(i <- numNodes - inputs.size - 1 to 0 by -1){
            val hashList = new ArrayBuffer[String]()
            tree(i).children.map { j =>hashList += tree(j).hash.toString}
            val inString: String = hashList.mkString("")
            tree(i).data = Message(inString, m.p.t)
        }

        tree 
    }
}

class MerkleTreeModel(m: MerkleParams, val nodes: ArrayBuffer[Node]){

    def apply(origContents: Seq[Message]): BigInt = {
        val tree = MerkleTreeModel.BuildTree(m, origContents)
        tree(0).hash
    }
}