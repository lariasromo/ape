package com.libertexgroup.ape.writers.s3.fromS3Files.noneBacked

import com.libertexgroup.ape.Reader
import com.libertexgroup.ape.readers.s3.{S3FileReader, S3FileWithContent}
import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Writers[ SConfig <: S3Config :Tag ](reader: S3FileReader[SConfig]) {

  def avro[T:SchemaFor :Encoder :Decoder :ClassTag :Tag]: Reader[Any, SConfig, S3FileWithContent[T]] =
    Reader.unitReader(reader --> new AvroPipe[T, SConfig])

  def jsonLines[T  :SchemaFor :Encoder :Decoder :ClassTag :Tag]
    (implicit decode: String => T): Reader[Any, SConfig, S3FileWithContent[T]] =
    Reader.unitReader(reader --> new JsonLinesPipe[T, SConfig])

  def jsonLinesCirce[T  :SchemaFor :Encoder :Decoder :ClassTag :Tag : io.circe.Decoder]:
    Reader[Any, SConfig, S3FileWithContent[T]] = Reader.unitReader(reader --> new JsonLinesCircePipe[T, SConfig])

  def text: Reader[Any, SConfig, S3FileWithContent[String]] = Reader.unitReader(reader --> new TextPipe[SConfig])

}