package poseidon

import chisel3._ 
import scala.collection.mutable.ArrayBuffer
import scala.collection.script.Message

//Inspiration for implementation taken
//from https://blog.genuine.com/2020/02/merkle-tree-implementation-in-scala/

case class MerkleParams(p: PoseidonParams, numChild: Int = 2){

}


class Node(data: BigInt, children: ArrayBuffer[Option[Node]]) {
    this.data = data
    this.children = children
}

object MerkleTreeModel{
    def BuildTree(leafNodes: ArrayBuffer[Message]): ArrayBuffer[Node] = {

    }
}

class MerkleTreeModel(m: MerkleParams, val nodes: ArrayBuffer[Node]){

    def apply(origContents: ArrayBuffer[Message]): BigInt = {
        val tree = BuildTree(origContents)
        tree(0).data
    }
}