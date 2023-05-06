package com.libertexgroup.pipes.s3.fromS3Files.zioBacked

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.S3Config
import com.libertexgroup.readers.s3.{S3FileReader, S3FileWithContent}
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Pipes[ SConfig <: S3Config :Tag ](reader: S3FileReader[SConfig]) {

  def avro[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Reader[Any, SConfig, S3FileWithContent[T]] =
    Reader.UnitReader(reader --> new AvroPipe[T, SConfig])

  def jsonLines[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]
    (implicit decode: String => T): Reader[Any, SConfig, S3FileWithContent[T]] =
    Reader.UnitReader(reader --> new JsonLinesPipe[T, SConfig])

  def jsonLinesCirce[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag : io.circe.Decoder]:
    Reader[Any, SConfig, S3FileWithContent[T]] = Reader.UnitReader(reader --> new JsonLinesCircePipe[T, SConfig])

  def text: Reader[Any, SConfig, S3FileWithContent[String]] = Reader.UnitReader(reader --> new TextPipe[SConfig])
}