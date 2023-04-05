package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import purecsv.safe._
import purecsv.safe.converter.Converter
import zio.ZIO
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[writers] class CsvWriter[ZE, T: ClassTag](sep: String = ",")(implicit rfc: Converter[T,Seq[String]])
 extends S3Writer[ZE with S3 with S3Config, ZE, T, T] {
  override def apply(stream: ZStream[ZE, Throwable, T]):
  ZIO[ZE with S3 with S3Config, Throwable, ZStream[ZE, Throwable, T]] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      bytesStream = stream
        .map(a => rfc.to(a).mkString(sep) + "\n")
        .map(_.getBytes)
        .flatMap(bytes => ZStream.fromIterable(bytes))
      fileName <- zio.Random.nextUUID
      _ <- multipartUpload(
        bucket,
        s"${location}/${fileName}.csv",
        bytesStream,
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield stream
}