package ape.s3.pipes.fromS3Files.zioBacked

import ape.s3.configs.S3Config
import ape.s3.pipes.fromS3Files.{S3ContentPipe, S3FilePipe, S3WithBackPressure}
import ape.s3.readers.S3FileWithContent
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */

class JsonLinesCircePipe[
  T>:Null :io.circe.Decoder :SchemaFor :Encoder :Decoder :ClassTag :Tag,
  Config <: S3Config :Tag
] extends S3ContentPipe[Config, Any, T ] {
  override protected[this] def pipe(i: ZStream[Any, Throwable, S3ObjectSummary]):
    ZIO[Config, Throwable, ZStream[Any, Throwable, S3FileWithContent[T]]] = for {
    data <- S3FilePipe.jsonLinesCircePipe(i)
  } yield data.map(S3WithBackPressure.zio[T].backPressure(_))

}