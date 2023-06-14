# ReaderOps
The `Reader` interface offers a series of operators that makes it easy to interact with the underlying stream without
creating new Pipes.

## Unit reader
This is a very handy method to create readers from an initial `ZStream`

```scala
import ape.reader.Reader
import zio.stream.ZStream

val inputStream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)
val reader: Reader[Any, Any, Int] = Reader.UnitReader(inputStream)
```

## Reader monad operations
- `map` or `withTransform`: Will produce a new Reader with a transformation applied. 
```scala
import ape.reader.Reader
import zio.stream.ZStream

val inputStream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)
val reader: Reader[Any, Any, Int] = Reader.UnitReader(inputStream)
val newReader: Reader[Any, Any, String] = reader.map(_.toString)
```

- `**`: Will produce a new Reader with a transformation implicit applied.
```scala
import ape.reader.Reader
import zio.stream.ZStream

val inputStream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)
val reader: Reader[Any, Any, Int] = Reader.UnitReader(inputStream)

implicit val t: Int => String = i => i.toString
val newReader: Reader[Any, Any, String] = reader.**[String]
```

- `mapZ` or `withZTransform`: Will produce a new Reader with a transformation applied. Although the transformation 
  is applied directly to the underlying `ZStream`
```scala
import ape.reader.Reader
import zio.stream.ZStream

val inputStream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)
val reader: Reader[Any, Any, Int] = Reader.UnitReader(inputStream)
val newReader: Reader[Any, Any, String] = reader.mapZ(_.map(_.toString))
```

- `***`: Will produce a new Reader with a transformation applied. Although the implicit transformation
  is applied directly to the underlying `ZStream`
```scala
import ape.reader.Reader
import zio.stream.ZStream

val inputStream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)
val reader: Reader[Any, Any, Int] = Reader.UnitReader(inputStream)

implicit val t: ZStream[Any, Throwable, Int] => ZStream[Any, Throwable, String] = s => s.map(_.toString)
val newReader: Reader[Any, Any, String] = reader.***
```

- `safeGet`: will try to convert a `Reader` of `Option[T]` into a `Reader` of `T`

```scala
import ape.reader.Reader
import zio.stream.ZStream

case class Example(name: String)

val inputStream: ZStream[Any, Nothing, Option[Example]] =
  ZStream(Some(Example("a")), Some(Example("b")), Some(Example("c")))
val reader: Reader[Any, Any, Option[Example]] = Reader.UnitReader(inputStream)

val newReader: Reader[Any, Any, Example] = reader.safeGet[Example]
```
- `as`: an alias for `safeGet`
```scala
import ape.reader.Reader
import zio.stream.ZStream

case class Example(name: String)

val inputStream: ZStream[Any, Nothing, Option[Example]] =
  ZStream(Some(Example("a")), Some(Example("b")), Some(Example("c")))
val reader: Reader[Any, Any, Option[Example]] = Reader.UnitReader(inputStream)

val newReader: Reader[Any, Any, Example] = reader.as[Example]
```
- `filter`: Will use the `ZStream` filter to filter out values based on a condition
```scala
import ape.reader.Reader
import zio.stream.ZStream

case class Example(name: String)

val inputStream: ZStream[Any, Nothing, Option[Example]] =
  ZStream(Some(Example("a")), Some(Example("b")), Some(Example("c")))
val reader: Reader[Any, Any, Option[Example]] = Reader.UnitReader(inputStream)

val newReader: Reader[Any, Any, Example] = reader.as[Example].filter(_.name.equals("a"))
```

## Building the pipe
- `-->`: This is the core of the pipeline, this method will concatenate a `Reader` with a `Pipe`. Producing a `ZStream`.

```scala
import ape.reader.Reader
import zio.stream.ZStream

val inputStream: ZStream[Any, Nothing, String] = ZStream("a", "b", "c")
val reader: Reader[Any, Any, String] = Reader.UnitReader(inputStream)
val writer = ape.misc.Pipes.pipes.console.ofString

val pipe: ZStream[Any, Throwable, String] = reader --> writer
```
- `->>`: This method will concatenate the reader and writer producing an effect that will `runDrain` the resulting 
  stream, useful to just plug the pipeline into the main method

```scala
import ape.reader.Reader
import zio.ZIO
import zio.stream.ZStream

val inputStream: ZStream[Any, Nothing, String] = ZStream("a", "b", "c")
val reader: Reader[Any, Any, String] = Reader.UnitReader(inputStream)
val writer = ape.misc.Pipes.pipes.console.ofString

val pipe: ZIO[Any, Throwable, Unit]  = reader ->> writer
```