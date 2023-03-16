package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.ZIO
import zio.s3.{S3, S3ObjectSummary, streamLines}
import zio.stream.ZStream

import scala.reflect.ClassTag


/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[readers] class TypedParquetReader[T >:Null: SchemaFor :Encoder :Decoder :ClassTag]
  extends S3Reader[S3Config, S3Config with S3, S3FileWithContent[T]] {
  override def apply: ZIO[S3FileReaderService with S3Config, Throwable,
    ZStream[S3Config with S3, Throwable, (S3ObjectSummary, ZStream[S3, Throwable, T])]] = for {
    s3FilesQueue <- fileStream
    stream = s3FilesQueue.mapZIO(file => for {
      stream <- readParquet[T](file)
    } yield (file, stream))
  } yield stream
}
