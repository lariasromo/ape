# S3 file readers

This is a special type of reader that works neatly with s3 files where we need to have a pipeline that reads files 
from S3 in a stream or batch mode.

There are 3 types of readers: 
 - `fileReaderSimple`: This reader will take a location string corresponding to an S3 location and list all files 
   forming a new stream
```scala
import ape.s3.configs.S3Config
import ape.s3.readers.S3FileReader

val reader: S3FileReader[S3Config]  = ape.s3.Readers.fileReader[S3Config].fileReaderSimple("path/to/files")
```
 - `fileReaderContinuous`: This reader will an effect that from a `ZonedDateTime` will form an S3 location and list 
   files from such location, it also takes a `zio.Duration` that lets the pipeline know how often it will look onto 
   S3. This reader keeps an internal hashmap to track already read files and just read new files.

```scala
import ape.s3.configs.S3Config
import ape.s3.readers.S3FileReader
import zio.Config.Duration
import zio.{ZIO, durationInt}

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

val zero: Int => String = i => if (i < 10) s"0$i" else i.toString
val pattern: ZIO[S3Config, Nothing, ZonedDateTime => List[String]] = ZIO.succeed { zdt =>
   val dt = LocalDateTime.ofInstant(zdt.toInstant, ZoneOffset.UTC)
   s"path/to/file" +
           s"/year=${zero(dt.getYear)}" +
           s"/month=${zero(dt.getMonthValue)}" +
           s"/day=${zero(dt.getDayOfMonth)}" +
           s"/hour=${zero(dt.getHour)}"
}
val reader: S3FileReader[S3Config] = ape.s3.Readers.fileReader[S3Config].fileReaderContinuous(pattern)
```

 - `fileReaderBounded`: This reader will an effect that from a `ZonedDateTime` will form an S3 location and list
   files from such location, it also takes a start and end `ZonedDateTime` and a step `zio.Duration` that increased 
   starting time `step` times until it reaches the end time. Use this for batch mode. 

```scala
import ape.s3.configs.S3Config
import ape.s3.readers.S3FileReader
import zio.Config.Duration
import zio.{ZIO, durationInt}

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

val zero: Int => String = i => if (i < 10) s"0$i" else i.toString
val pattern: ZIO[S3Config, Nothing, ZonedDateTime => List[String]] = ZIO.succeed { zdt =>
   val dt = LocalDateTime.ofInstant(zdt.toInstant, ZoneOffset.UTC)
   s"path/to/file" +
           s"/year=${zero(dt.getYear)}" +
           s"/month=${zero(dt.getMonthValue)}" +
           s"/day=${zero(dt.getDayOfMonth)}" +
           s"/hour=${zero(dt.getHour)}"
}
val startTime = ZonedDateTime.of(2023, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)
val endTime = ZonedDateTime.of(2023, 2, 1, 1, 0, 0, 0, ZoneOffset.UTC)
val step = 1.day
val reader: S3FileReader[S3Config] = 
   ape.s3.Readers.fileReader[S3Config].fileReaderBounded(pattern, startTime, endTime, step)
```

The result of the three above effects will only produce a reader `S3FileReader` with the reference of the S3 file 
(`S3ObjectSummary`). The `S3FileReader` monad also contains a useful method to read the content of such files.
 - `avro`: Reads the content of the S3 file as avro
 - `text`: Reads the content of the S3 file as text
 - `jsonLines`: Reads the content of the S3 file as json lines into an object `T` defined as a case class using an 
   implicit conversion  `String => T`
 - `jsonLinesCirce`: Reads the content of the S3 file as json lines into an object `T` defined as a case class using 
   circe as decoder

In all cases above, the resulting type will contain the same file reference `S3ObjectSummary`

```scala
import ape.reader.Reader
import ape.s3.configs.S3Config
import ape.s3.readers.S3FileReader
import io.circe.Decoder
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import io.circe.generic.semiauto.deriveDecoder

case class Person(name: String)
object Person {
   implicit val fromStr: String => Person = name => Person(name)
   implicit val jsonDecoder: Decoder[Person] = deriveDecoder[Person]
}

type S3FileWithContent[T] = (S3ObjectSummary, ZStream[Any, Throwable, T])
val reader = ape.s3.Readers.fileReader[S3Config].fileReaderSimple("path/to/files")

val avroReader: Reader[Any, S3Config, S3FileWithContent[Person]] = 
   reader.readFiles.avro[Person]
   
val textReader: Reader[Any, S3Config, S3FileWithContent[String]] = 
   reader.readFiles.text

val jsonLinesReader: Reader[Any, S3Config, S3FileWithContent[Person]] = 
   reader.readFiles.jsonLines[Person]

val jsonLinesCirceReader: Reader[Any, S3Config, S3FileWithContent[Person]] = 
   reader.readFiles.jsonLinesCirce[Person]
```