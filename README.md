Poseidon Hash Hardware Generator
=======================
 This repo contains a scala model as well as a work-in-progress implementation of a hardware generator for [Poseidon Hash](https://www.poseidon-hash.info/). The Poseidon hash function is a cryptographic hash function operating on GF(p) objects. The link to the original paper can be found [here](https://eprint.iacr.org/2019/458.pdf). Our scala model is based on a [reference implementation](https://extgit.iaik.tugraz.at/krypto/hadeshash/-/blob/master/code/poseidonperm_x5_254_3.sage) provided by the authors.

#### Steps to Run

Once the repo has been cloned and you are in the top-level directory, simply type **sbt test** in your terminal and all the written test cases will run. 

### Completed Feature Documentation
Currently, the completed features include a working Scala model which takes in an arbitrary string as an input
and computes the hash by performing multiple rounds of the three step permutation function listed in the paper.

Our Chisel implementation of the hardware generator can also perform the above hash for a fixed number of rounds and fixed alpha. 

### Work In Progress
We are working on building the model and a generator for a Merkle tree. In this Merkle tree, each parent node will be computed by hashing 
and merging it's child nodes. We plan on multi-layer 2-to-1 or 4-to-1 tree and allow for various parallelization schemes for computing the 
parent nodes.
