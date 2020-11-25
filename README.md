# Swarm

Library written in pure Kotlin to use for parallel computations with map programming paradigm. It may run on top of MPI (for multi-computer computation) or threads (for single maching).

## Installation

Include into your Gradle build script the following line:

```Gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation group: 'com.github.inforion', name: 'swarm', version: '0.2.0'
}
```

That's it you are ready to go (for threads). If you want real cluster computation you should also install underlying MPI library. Swarm contains and uses built bindings for OpenMPI library. For linux based systems package `openmpi-bin` should be installed.
Note that setup openmpi for cluster computation is not for faint of heart and may require additional packages.

## Parallel concept of Swarm

In cluster programming (and for threads-case that simulates cluster) often used the next terms: master and slave nodes.
- master node - is the main node that get control first of all
- slave node - is node that wait for task (from master map command) and then do a job

In Swarm we always have one master node and as many slaves nodes as specified during Swarm starts.

Also in whole program we have three main parts:
- common code - executed for all nodes (master and slaves)
- master code - executed only for master node
- slave code - exectued only for slaves node

## Usage

First we should create a 'split point' or 'entry point' for Swarm. It can be done using one of the function: `threadsSwarm` or `mpiSwarm`, depends on what underlying distibuted computation layer we want to use, e.g.:

```Kotlin

// this code executes for all nodes: master and slaves

threadsSwarm(4) {  // here we specify 4 slaves nodes
    // now we are part of the swarm
    // this code executes only for master node
}
```

After we become a part of swarm we have two main type of possible actions: interaction with other nodes and map-computation itself.
If in computation task no pre-computation is needed, i.e. no **context** for slaves required, then `map` function can be used.

```Kotlin
threadsSwarm(size) { swarm ->
    // this code executes only for master node
    val result = listOf("ab", "cd", "dd")
        .parallelize(swarm)  // creates a special wrapper ParallelIterable for collection for parallization
        .map {  // API is simpilar for others map-like function
            // this code will be executed on slaves nodes in parallel
            it.toUpperCase()
        }
    
    // result = listOf("AB", "CD", "DD")
}
```

But in some cases we may need to setup context for all nodes before computation, then `context` may be used on Swarm object

```Kotlin
// Context class, Heavy is some heavy to compute object and we should do it once before map
class Context(val node: Int, val heavy: Heavy)

threadsSwarm(size) { swarm ->
    swarm.context { 
        val heavy = initializeHeavy(...)
        Context(it, heavy)
    }
    
    swarm
        .get { context: Context -> context.heavy.someField }  // using get we could collect information from all slaves
        .forEach { println(it) }  // this executes only on master (simple forEach)

    val result = listOf("ab", "cd", "dd")
        .parallelize(swarm)
        .mapContext { context: Context, value ->  // use of mapContext to also get a previously set context among with value
            context.heavy.run(value)
        }
    
    // result = listOf context.heavy.run(...) on "ab", "cd", "dd"
}
```

Besides `map` and `mapContext` methods for `ParallelIterable`, a `filter` method available.
Also you can find a number of usage example is in the `SwarmTests`. 
