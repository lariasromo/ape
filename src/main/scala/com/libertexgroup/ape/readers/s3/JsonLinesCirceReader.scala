package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import io.circe.{Decoder, jawn}
import zio.ZIO
import zio.s3.S3
import zio.stream.ZStream

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[readers] class JsonLinesCirceReader[T: Decoder :ClassTag](location:String)
  extends S3Reader[S3 with S3Config, S3, T] {

  override def apply: ZIO[S3 with S3Config, Throwable, ZStream[S3, Throwable, T]] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      stream <- readPlainText(bucket, location)
    } yield stream.map(l => jawn.decode[T](l).toOption).filter(_.isDefined).map(_.get)
}
