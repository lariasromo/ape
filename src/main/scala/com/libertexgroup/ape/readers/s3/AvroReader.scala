package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.readers.Reader
import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.ZIO
import zio.s3.S3
import zio.stream.ZStream

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[readers] class AvroReader[T >:Null :SchemaFor :Decoder :Encoder :ClassTag]
  extends S3Reader[S3 with S3Config, S3 with S3Config, S3FileWithContent[T]] {
  override def apply: ZIO[S3FileReaderService with S3 with S3Config, Throwable, ZStream[S3 with S3Config, Throwable, S3FileWithContent[T]]] =
    for {
      config <- ZIO.service[S3Config]
      s3FilesQueue <- fileStream
      stream = s3FilesQueue.mapZIO(file => readBytes[T](file).map(x => (file, x)))
      newStream = if(config.enableBackPressure) readWithBackPressure(stream) else stream
    } yield newStream

}
