package com.libertexgroup.ape.writers.s3.fromS3Files.redisBacked

import com.libertexgroup.ape.Reader
import com.libertexgroup.ape.readers.s3.{S3FileReader, S3FileWithContent}
import com.libertexgroup.configs.{RedisConfig, S3Config}
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Writers[ SConfig <: S3Config :Tag, RConfig <: RedisConfig :Tag ](reader: S3FileReader[SConfig]) {

  def avro[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]:
    Reader[Any, SConfig with RConfig, S3FileWithContent[T]] =
      Reader.unitReader(reader --> new AvroPipe[T, SConfig, RConfig])

  def jsonLines[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag](implicit decode: String => T):
    Reader[Any, SConfig with RConfig, S3FileWithContent[T]]  =
      Reader.unitReader(reader --> new JsonLinesPipe[T, SConfig, RConfig])

  def jsonLinesCirce[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag : io.circe.Decoder]:
    Reader[Any, SConfig with RConfig, S3FileWithContent[T]]  =
      Reader.unitReader(reader --> new JsonLinesCircePipe[T, SConfig, RConfig])

  def text: Reader[Any, SConfig with RConfig, S3FileWithContent[String]] =
    Reader.unitReader(reader --> new TextPipe[SConfig, RConfig])

}