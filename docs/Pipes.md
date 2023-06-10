Pipes
------
- [List of Pipes](PipeList.md)
- [Pipe operations](PipeOps.md)
## Description

The class Pipe is an abstraction of an entity that will convert a stream of elements `T0` execute and action and give 
back a new type `T1`


```text


   ┌────────────────┐        ┌─────────────────┐      ┌─────────────────┐
   │                │        │                 │      │                 │
   │                │        │                 │      │                 │
   │     Reader     │   T0   │     Pipe 1      │ T1   │     Pipe 2      │ T2
   │                ├────────►                 ├──────►                 ├─────────►
   │                │        │                 │      │                 │
   │                │        │                 │      │                 │
   └────────────────┘        └─────────────────┘      └─────────────────┘


```

To create a new `Pipe` the class should implement a method `pipe` 
```scala
import zio.stream.ZStream
protected[this] def pipe(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T]]
```
This method will take a stream of `T0` and effectfully create a new stream of `T`

### Example

```scala
import ape.pipe.Pipe
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class ExamplePipe[E, ET, T: ClassTag] extends Pipe[E, ET, T, T] {
  override protected[this] def pipe(i: ZStream[ET, Throwable, T]): ZIO[E, Throwable, ZStream[ET, Throwable, T]] =
    ZIO.succeed(i.tap(r => zio.Console.printLine(r.toString)))
}
```
