# Back pressure

These are a special type of pipes that allows existing pipes to be read with a significant increase of performance.

The mechanism is simple, for an input pipe it will produce a new pipe where all elements will be stored into an 
into a queue provided by an underlying mechanism.

For obvious reasons this approach will make the application utilize more memory but since such memory can be 
provided by an external memory database provider (redis) we can easily scale off our application easily.

The back pressure pipe can be enabled in 2 modes:
 - finite: the underlying cache will be created as long as the stream lives, useful for pipes that have a finite 
   number of elements and when we handle multiple pipes where not all of them need to be offloaded allowing to 
   gracefully shut-down the cache when is not used.
   To implement this approach, all elements from an incoming pipe should be first written to cache then popped out as they are read 
   which increases greatly (but just temporarily) the size of the used cache. 
    
 - infinite: the underlying cache will remain open as long as the stream from both pipes is consumed. This 
   approach doesn't increase the size of the used cache but keeps the cache open at all times.

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe

case class Ex1(name: String)

case class Ex2(name: String)

val pipe1: Pipe[Any, Any, String, Ex1] = Pipe.UnitTWriter(name => Ex1(name))
val pipe2: Pipe[Any, Any, Ex1, Ex2] = Pipe.UnitTWriter(e1 => Ex2(e1.name))

val backPressurePipe = Ape.pipes.misc.backPressure.finite[Ex1]
val newPipe: Pipe[Any, Any, String, Ex2] = (pipe1 --> backPressurePipe --> pipe2)
```