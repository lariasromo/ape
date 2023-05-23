package ape.s3.pipes.fromS3Files.redisBacked

import ape.reader.Reader
import ape.redis.configs.RedisConfig
import ape.s3.configs.S3Config
import ape.s3.readers.{S3FileReader, S3FileWithContent}
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Pipes[ SConfig <: S3Config :Tag, RConfig <: RedisConfig :Tag ](reader: S3FileReader[SConfig]) {

  def avro[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]:
    Reader[Any, SConfig with RConfig, S3FileWithContent[T]] =
      Reader.UnitReader(reader --> new AvroPipe[T, SConfig, RConfig])

  def jsonLines[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag](implicit decode: String => T):
    Reader[Any, SConfig with RConfig, S3FileWithContent[T]]  =
      Reader.UnitReader(reader --> new JsonLinesPipe[T, SConfig, RConfig])

  def jsonLinesCirce[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag : io.circe.Decoder]:
    Reader[Any, SConfig with RConfig, S3FileWithContent[T]]  =
      Reader.UnitReader(reader --> new JsonLinesCircePipe[T, SConfig, RConfig])

  def text: Reader[Any, SConfig with RConfig, S3FileWithContent[String]] =
    Reader.UnitReader(reader --> new TextPipe[SConfig, RConfig])

}