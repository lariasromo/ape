package com.libertexgroup.pipes.s3.fromS3Files.redisBacked

import com.libertexgroup.configs.{RedisConfig, S3Config}
import com.libertexgroup.readers.s3.S3FileWithContent
import com.libertexgroup.utils.Utils.reLayer
import com.libertexgroup.pipes.s3.fromS3Files.{S3ContentPipe, S3FilePipe, S3WithBackPressure}
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 * (file, readPlainText(config.compressionType, file).map(decode))
 */
class JsonLinesPipe[T>:Null :ClassTag :SchemaFor :Encoder :Decoder :Tag,
  Config <: S3Config :Tag,
  RConfig <: RedisConfig :Tag
] (implicit decode: String => T) extends S3ContentPipe[Config with RConfig, Any, T] {
  override protected[this] def pipe(i: ZStream[Any, Throwable, S3ObjectSummary]):
  ZIO[Config with RConfig, Throwable, ZStream[Any, Throwable, S3FileWithContent[T]]] = for {
    redis <- reLayer[RConfig]
    s <- S3FilePipe.jsonLinesPipe(i, decode)
  } yield s.mapZIO(S3WithBackPressure.redis[RConfig].backPressureZ[T](_).provideSomeLayer(redis))
}