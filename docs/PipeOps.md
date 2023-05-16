# Pipeops
The `Pipe` interface offers a series of operators that makes it easy to interact with the underlying stream without 
creating new Pipes.

## Unit Writers
The below methods will help build new pipes without creating new classes that extend from the `Pipe` interface, 
which allows us to have leaner code. 

- `UnitWriter`:

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import zio.ZIO
import zio.stream.ZStream

def transformation(input: ZStream[Any, Throwable, Int]) = 
  ZIO.succeed(input.map(_.toString))
  
val pipe: Pipe[Any, Any, Int, String] = 
  Pipe.UnitWriter(stream => transformation(stream))
```

- `UnitTWriter`:
```scala
import com.libertexgroup.ape.pipe.Pipe

val pipe: Pipe[Any, Any, Int, String] = 
  Pipe.UnitTWriter((input: Int) => input.toString)
```

- `UnitZWriter`:
```scala
import com.libertexgroup.ape.pipe.Pipe
import zio.stream.ZStream

val pipe: Pipe[Any, Any, Int, String] =
  Pipe.UnitZWriter((input: ZStream[Any, Throwable, Int]) => input.map(_.toString))
```

## Pipe monad operators
- `map` or `withTransform`: This operation will modify the type `T` of a pipe taking a transformation of type `T =>
  T2`, giving `T2` as a result of this operation.

```scala
import com.libertexgroup.ape.pipe.Pipe

case class Ex1(name: String)
case class Ex2(name: String)

val pipe: Pipe[Any, Any, String, Ex1] =
  Pipe.UnitTWriter((name: String) => Ex1(name))

val newPipe: Pipe[Any, Any, String, Ex2] = 
  pipe.map(ex1 => Ex2(ex1.name))
```

- `**`: This operator will apply the `map` operation using a transformation of type `T => T2` as an implicit. Giving
  a result type of `T2`.

```scala
import com.libertexgroup.ape.pipe.Pipe

case class Ex1(name: String)
object Ex1 {
  implicit val t: Ex1 => Ex2 = s => Ex2(s.name)
}
case class Ex2(name: String)

val pipe: Pipe[Any, Any, String, Ex1] = Pipe.UnitTWriter((name: String) => Ex1(name))
val newPipe: Pipe[Any, Any, String, Ex2] = pipe.**[Ex2]
```

- `mapZ` or `withZTransform`: This operation will work directly with the underlying stream taking a
  transformation of type `ZStream[E, Throwable, T] => ZStream[E, Throwable, T2]`, giving `T2` as a result of this
  operation. This operator is useful to unleash the power of the ZIO stream.

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import zio.Chunk
import zio.stream.ZStream

case class Example(lst: Chunk[String])

val pipe: Pipe[Any, Any, Example, Chunk[String]] = Pipe.UnitTWriter((e: Example) => e.lst)
val newPipe: Pipe[Any, Any, Example, String] = pipe.mapZ(s => s.flatMap(ZStream.fromChunk))

// or just
val newPipe2: Pipe[Any, Any, Example, String] = 
  Pipe.UnitTWriter((e: Example) => e.lst)
          .mapZ(s => s.flatMap(ZStream.fromChunk))
```

- `***`: This operator will apply the `mapZ` operation using a transformation of type `ZStream[E, Throwable, T] =>
  ZStream[E, Throwable, T2]` as an implicit. Giving a result type of `T2`.

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import zio.Chunk
import zio.stream.ZStream

case class Example(lst: Chunk[String])
implicit val t: ZStream[Any, Throwable, Chunk[String]] => ZStream[Any, Throwable, String] = 
  s => s.flatMap(ZStream.fromChunk)

val pipe: Pipe[Any, Any, Example, Chunk[String]] = Pipe.UnitTWriter((e: Example) => e.lst)
val newPipe = pipe.***[String]

// or just
val newPipe2 = Pipe.UnitTWriter((e: Example) => e.lst).***[String]
```

- `contramap`: This operation uses a given transformation `T0 => T` and will create a new Pipe from an existing one.
  This operator will give back the same output type of the original Pipe but taking a new value `T0` as input.

```scala
import com.libertexgroup.ape.pipe.Pipe

case class Ex1(name: String)
case class Ex2(name: String)

val pipe: Pipe[Any, Any, Ex1, Ex2] = Pipe.UnitTWriter((e1: Ex1) => Ex2(e1.name))
  
val newPipe: Pipe[Any, Any, String, Ex2] = 
  pipe.contramap[String](name => Ex1(name))
```

- `contramapZ`: This operation uses a given transformation `ZStream[E, Throwable, T0] => ZStream[E, Throwable, T]`
  and will create a new Pipe from an existing one. This operator will give back the same output type of the original
  Pipe but taking a new value `T0` as input.

```scala
import com.libertexgroup.ape.pipe.Pipe

case class Ex1(name: String)
case class Ex2(name: String)

val pipe: Pipe[Any, Any, Ex1, Ex2] = Pipe.UnitTWriter((e1: Ex1) => Ex2(e1.name))

val newPipe: Pipe[Any, Any, String, Ex2] = 
  pipe.contramapZ[String](s => s.map(name => Ex1(name)))
```

- `contramapZZ`: This operation uses a given transformation `T0 => ZIO[E, Throwable, T]` and will create a new Pipe
  from an existing one. This operator will give back the same output type of the original Pipe but taking a new
  value `T0` as input. This operator allows to have a pipe that produces a new pipe with an effect `E with E2`.

```scala
import com.libertexgroup.ape.pipe.Pipe
import zio.ZIO

case class Ex1(name: String)
case class Ex2(name: String)
case class SomeConfig(url: String)

val pipe: Pipe[Any, Any, Ex1, Ex2] = Pipe.UnitTWriter((e1: Ex1) => Ex2(e1.name))

val newPipe: Pipe[SomeConfig, Any, String, Ex2] =
  pipe.contramapZZ[SomeConfig, String](
    (s: String) => for {
      c <- ZIO.service[SomeConfig]
    } yield c.url + "?name=" + s
  ) 
```

- `contramapZZZ`: This operation uses a given transformation `ZStream[ZE, Throwable, T0] => ZIO[E2, Throwable,
  ZStream[ZE, Throwable, T]]` and will create a new Pipe from an existing one. This operator will give back the same
  output type of the original Pipe but taking a new value `T0` as input. This operator allows to have a pipe that
  produces a new pipe with an effect `E with E2`.

```scala
import com.libertexgroup.ape.pipe.Pipe
import zio.ZIO
import zio.stream.ZStream

case class Ex1(name: String)
case class Ex2(name: String)
case class SomeConfig(url: String)

val pipe: Pipe[Any, Any, Ex1, Ex2] = Pipe.UnitTWriter((e1: Ex1) => Ex2(e1.name))

val newPipe: Pipe[SomeConfig, Any, String, Ex2] =
  pipe.contramapZZZ[SomeConfig, String](
    (s: ZStream[Any, Throwable, String]) => for {
      c <- ZIO.service[SomeConfig]
    } yield s.map(name => c.url + "?name=" + name)
  ) 
```

## Pipe operations
From the below operations, each operation is a method of an existing pipe that creates a new pipe as a result.
- `<*>` or `cross`: Each incoming record will be broadcasted to 2 pipes with a default lag of 1, output is disregarded

```scala
import com.libertexgroup.ape.pipe.Pipe
import zio.Scope

case class Ex1(name: String)
case class Ex2(name: String)

val pipe1: Pipe[Any, Any, String, Ex1] = Pipe.UnitTWriter(name => Ex1(name))
val pipe2: Pipe[Any, Any, String, Ex2] = Pipe.UnitTWriter(name => Ex2(name))

val crossPipe: Pipe[Scope, Any, String, Any] = (pipe1 <*> pipe2)
```

- `<*` or `crossLeft`: Each incoming record will be broadcasted to 2 pipes with a default lag of 1, keeping just the 
  output of the first pipe.

```scala
import com.libertexgroup.ape.pipe.Pipe
import zio.Scope

case class Ex1(name: String)
case class Ex2(name: String)

val pipe1: Pipe[Any, Any, String, Ex1] = Pipe.UnitTWriter(name => Ex1(name))
val pipe2: Pipe[Any, Any, String, Ex2] = Pipe.UnitTWriter(name => Ex2(name))

val crossPipe: Pipe[Scope, Any, String, Ex1] = (pipe1 <* pipe2)
```
- `*>` or `crossRight`: Each incoming record will be broadcasted to 2 pipes first come first served, keeping just the
  output of the second pipe.

```scala
import com.libertexgroup.ape.pipe.Pipe
import zio.Scope

case class Ex1(name: String)
case class Ex2(name: String)

val pipe1: Pipe[Any, Any, String, Ex1] = Pipe.UnitTWriter(name => Ex1(name))
val pipe2: Pipe[Any, Any, String, Ex2] = Pipe.UnitTWriter(name => Ex2(name))

val newPipe: Pipe[Scope, Any, String, Ex2] = (pipe1 *> pipe2)
```
- `++` or `zip`: Each incoming record will be broadcasted to 2 pipes, giving as a result a tuple of both pipes 
  response types.

```scala
import com.libertexgroup.ape.pipe.Pipe
import zio.Scope

case class Ex1(name: String)
case class Ex2(name: String)

val pipe1: Pipe[Any, Any, String, Ex1] = Pipe.UnitTWriter(name => Ex1(name))
val pipe2: Pipe[Any, Any, String, Ex2] = Pipe.UnitTWriter(name => Ex2(name))

val newPipe: Pipe[Scope, Any, String, (Ex1, Ex2)] = (pipe1 ++ pipe2)
```
- `-->` or `concatenate`: Each incoming record will be sent to the first pipe, then the result of this first pipe 
  will be sent to the second pipe, giving as a result the output type of the second pipe.

```scala
import com.libertexgroup.ape.pipe.Pipe
import zio.Scope

case class Ex1(name: String)
case class Ex2(name: String)

val pipe1: Pipe[Any, Any, String, Ex1] = Pipe.UnitTWriter(name => Ex1(name))
val pipe2: Pipe[Any, Any, Ex1, Ex2] = Pipe.UnitTWriter(e1 => Ex2(e1.name))

val newPipe: Pipe[Any, Any, String, Ex2] = (pipe1 --> pipe2)
```
