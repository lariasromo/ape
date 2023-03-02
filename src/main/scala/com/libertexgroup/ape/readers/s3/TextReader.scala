package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import zio.ZIO
import zio.s3.S3
import zio.stream.ZStream

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[readers] class TextReader(location:String)
  extends S3Reader[S3 with S3Config, S3, String] {

  override def apply: ZIO[S3 with S3Config, Throwable, ZStream[S3, Throwable, String]] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      stream <- readPlainText(bucket, location)
    } yield stream
}
