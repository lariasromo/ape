package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.{Tag, ZIO}
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class AvroReader[
  T >:Null :SchemaFor :Decoder :Encoder :ClassTag,
  Config <: S3Config :Tag,
  AWSS3 <: S3 :Tag] extends S3Reader[
    S3FileReaderService[Config, AWSS3] with AWSS3 with Config,
    AWSS3 with Config,
    S3FileWithContent[T, AWSS3],
    AWSS3, Config
  ] {

  override def apply: ZIO[S3FileReaderService[Config, AWSS3] with Config, Throwable,
    ZStream[AWSS3 with Config, Throwable, S3FileWithContent[T, AWSS3]]] = for {
      config <- ZIO.service[S3Config]
      s3FilesQueue <- fileStream
      stream = s3FilesQueue.mapZIO(file => readBytes[T, AWSS3, Config](file).map(x => (file, x)))
      newStream = if(config.enableBackPressure) readWithBackPressure[T, AWSS3, Config](stream) else stream
    } yield newStream
}
