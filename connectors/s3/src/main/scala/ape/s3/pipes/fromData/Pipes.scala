package ape.s3.pipes.fromData

import ape.pipe.Pipe
import ape.s3.configs.S3Config
import ape.utils.Utils.:=
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag
import zio.s3.{S3, S3ObjectListing}

import scala.reflect.ClassTag

protected[s3] class Pipes[Config <: S3Config :Tag]() {
  class encoded[ZE] {
    def avro[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
    Pipe[ZE with S3 with Config, ZE, T, S3ObjectListing] = new AvroPipe[ZE, T, Config]
//    def parquet[T >: Null : SchemaFor : Encoder : Decoder : ClassTag]
//    (chunkSize: Int, duration: Duration): Pipe[ZE with S3 with Config, ZE, T, T] =
//      new ParquetPipe[ZE, T, Config](chunkSize, duration)
  }
  def encoded[ZE](implicit d1: ZE := Any) = new encoded[ZE]

  class text[ZE] {
    def default: Pipe[ZE with S3 with Config, ZE, String, S3ObjectListing] = new TextPipe[ZE, Config]
    def csv[T: ClassTag](
                          sep: String = ",",
                          order:Option[Seq[String]]=None
                        ): Pipe[ZE with S3 with Config, ZE, T, S3ObjectListing] = new CsvPipe[ZE, T, Config](sep, order)
  }
  def text[ZE](implicit d1: ZE := Any) = new text[ZE]

  class jsonLines[ZE] {
    def default[T: ClassTag](implicit e: T => String): Pipe[ZE with S3 with Config, ZE, T, S3ObjectListing] =
      new JsonLinesPipe[ZE, T, Config]

    def circe[T: io.circe.Encoder: ClassTag]: Pipe[ZE with S3 with Config, ZE, T, S3ObjectListing] =
      new JsonLinesCircePipe[ZE, T, Config]
  }
  def jsonLines[ZE](implicit d1: ZE := Any) = new jsonLines[ZE]
}
