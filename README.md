Poseidon Hash Hardware Generator
=======================
 This repo contains a scala model as well as a work-in-progress implementation of a hardware generator for [Poseidon Hash](https://www.poseidon-hash.info/). The Poseidon hash function is a cryptographic hash function operating on GF(p) objects. The link to the original paper can be found [here](https://eprint.iacr.org/2019/458.pdf). Our scala model is based on a [reference implementation](https://extgit.iaik.tugraz.at/krypto/hadeshash/-/blob/master/code/poseidonperm_x5_254_3.sage) provided by the authors.

#### Steps to Run

Once the repo has been cloned and you are in the top-level directory, simply enter **`sbt test`** to run all the provided test cases. 

### Completed Feature Documentation
Currently, the completed features include:
* Working Scala model which takes in an arbitrary string as an input and computes the hash. The algorithm involves transforming the given message into `t chunks of 32B`. 
Following the initialization, three steps are performed:
1. Add Round Constants: Each element in the state vector is added with a provided large value.
3. S-Box: Take the modular α-th power of each element in the state vector. Typically, this value is either 3 or 5. If the round is a partial round, only take the exponent of the first element.
4. Mix Layer: Perform a matrix multiplication of the state vector with a provided `t*t` MDS matrix and store the result back into the state vector. 

* Currently, our code works for **`t = 3`** and **`α = 5`**.

* Chisel implementation of the hardware generator for Poseidon Permutation `x5_254_3`. The current implementation parallelizes the Mix-layer matrix multiplication.

### Work In Progress 
* Building the model and a generator for a Merkle tree. In this Merkle tree, each parent node will be computed by hashing 
and merging it's child nodes. The hash function used will be the aforementioned Poseidon. We plan on building a multi-layer 2-to-1 or 4-to-1 tree and allow for various parallelization schemes for computing the parent nodes.

* Various optimizations for the existing generators. These include more efficient modular arithmetic and multi-cycle input transfer.

* Allow for different t-values in permutation. This will result in support for multiple hashes in the Poseidon family.
