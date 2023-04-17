package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class AvroReader[T >:Null :SchemaFor :Decoder :Encoder :ClassTag, Config <: S3Config :Tag]
  extends S3Reader[
    Config,
    S3 with Config,
    S3FileWithContent[T],
    Config
  ] {

  override protected[this] def read: ZIO[S3FileReaderService[Config] with Config, Throwable,
    ZStream[S3 with Config, Throwable, (S3ObjectSummary, ZStream[S3, Throwable, T])]] =
    for {
      config <- ZIO.service[Config]
      s3FilesQueue <- fileStream
      stream = s3FilesQueue.mapZIO(file => readBytes[T, Config](file).map(x => (file, x)))
      newStream = if(config.enableBackPressure) readWithBackPressure[T, Config](stream) else stream
    } yield newStream
}
