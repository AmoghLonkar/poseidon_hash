package poseidon

import chisel3._ 
import scala.collection.mutable.ArrayBuffer

case class MerkleParams(p: PoseidonParams, numChild: Int = 2){

}


class Node(m: MerkleParams, data: BigInt, child: Int) {
    var data = data
}

object MerkleTreeModel{
   
    def log2ceil(in: Int): Int = { (log10(int)/log10(2)).ceil.toInt }

    def BuildTree(m: MerkleParams, inputs: Seq[Message]): ArrayBuffer[Node] = {
        //Initialize tree
        val tree = new ArrayBuffer[Node]()
        val numNodes: Int = (0 to log2ceil(leafData.size)).map( i => (leafData.size/pow(m.numChild, i)).ceil).sum.toInt
        (0 until numNodes).foreach(i => tree += Node(m, BigInt(0), ))

        //Get Hash values of each input and convert to leaf nodes
        val hashList = Seq.fill(inputs.size)(new PoseidonModel(m.p))
        val leafData = hashList.zip(inputs).map { case(a, b) => a(b) }

        (0 until inputs.size).foreach(i => tree(i).data = leafData(i))

        //Iterate up the tree to get 
    }
}

class MerkleTreeModel(m: MerkleParams, val nodes: ArrayBuffer[Node]){

    def apply(origContents: Seq[Message]): BigInt = {
        val tree = BuildTree(m, origContents)
        tree(0).data
    }
}