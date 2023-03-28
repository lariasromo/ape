package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import org.apache.avro.generic.GenericRecord
import zio.ZIO
import zio.s3.S3
import zio.stream.ZStream


/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[readers] class ParquetReader extends S3Reader[Any, S3Config with S3, GenericRecord] {
  override def apply: ZIO[S3FileReaderService, Nothing, ZStream[S3Config with S3, Throwable, GenericRecord]] =
    for {
      s3FilesQueue <- fileStream
      s = s3FilesQueue.mapZIO(readParquetGenericRecords).flatMap(x=>x)
    } yield s
}
