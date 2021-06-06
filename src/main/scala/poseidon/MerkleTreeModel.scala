package poseidon

import chisel3._ 
import scala.math._
import scala.collection.mutable.ArrayBuffer

case class MerkleParams(p: PoseidonParams, numInputs: Int, treeRadix: Int = 2){
    require(treeRadix <= numInputs)
    require(treeRadix == 2 || treeRadix == 4)
    def lognceil(in: Int, size: Int): Int = { (log10(in)/log10(size)).ceil.toInt }
    val numNodes: Int = (0 to lognceil(numInputs, treeRadix)).map( i => (numInputs/pow(treeRadix, i)).ceil).sum.toInt
}


case class Node(m: MerkleParams, val data: Message, children: Seq[Int]) {
    val hashFn = new PoseidonModel(m.p)
    var hash = hashFn(data)

    def computeHash(in_val: Message) = {
        hash = hashFn(in_val)
    } 
}

object MerkleTreeModel{
   
    def BuildTree(m: MerkleParams, inputs: Seq[Message]): ArrayBuffer[Node] = {
        //Initialize tree
        val tree = new ArrayBuffer[Node]()

        for(i <- 0 until m.numNodes){
            if(i < m.numNodes - inputs.size){
                tree += Node(m, Message("", m.p.t), Seq.tabulate(m.treeRadix)(j => m.treeRadix*i + j+1))
            } 
            else {
                tree += Node(m, inputs(i - (inputs.size - m.treeRadix) - 1), Seq.fill(1)(i))
            }
        }
        
        //Iterate up the tree to get final hash
        for(i <- m.numNodes - inputs.size - 1 to 0 by -1){
            val hashList = new ArrayBuffer[BigInt]
            tree(i).children.map { j =>hashList += tree(j).hash}
            val inString: String = hashList.reduce{_^_}.toString
            tree(i).computeHash(Message(inString, m.p.t))
        }
        tree 
    }
}

class MerkleTreeModel(m: MerkleParams){

    def apply(origContents: Seq[Message]): BigInt = {
        val tree = MerkleTreeModel.BuildTree(m, origContents)
        tree(0).hash
    }
}