package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.readers.Reader
import com.libertexgroup.configs.S3Config
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.hadoop.util.HadoopInputFile
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, ZIO}


/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
class TextReader extends Reader[S3 with S3Config, S3, String] {

  override def apply: ZIO[S3 with S3Config, Throwable, ZStream[S3, Throwable, String]]
  =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      stream <- readPlainText(bucket, location)
    } yield stream
}
