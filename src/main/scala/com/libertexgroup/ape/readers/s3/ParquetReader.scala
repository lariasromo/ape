package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import org.apache.avro.generic.GenericRecord
import zio.{Tag, ZIO}
import zio.s3.S3
import zio.stream.ZStream


/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class ParquetReader[E, AWSS3 <: S3 :Tag, Config <: S3Config :Tag]
  extends S3Reader[E, Config with AWSS3, GenericRecord, AWSS3, Config] {
  override def apply: ZIO[S3FileReaderService[Config, AWSS3], Nothing, ZStream[Config with AWSS3, Throwable, GenericRecord]] =
    for {
      s3FilesQueue <- fileStream
    } yield s3FilesQueue.mapZIO(readParquetGenericRecords).flatMap(x=>x)
}
