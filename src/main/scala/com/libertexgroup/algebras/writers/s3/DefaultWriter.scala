package com.libertexgroup.algebras.writers.s3

import com.libertexgroup.configs.S3Config
import zio.s3.{MultipartUploadOptions, S3, UploadOptions, multipartUpload}
import zio.stream.ZStream
import zio.{ZIO, s3}

class DefaultWriter[E] extends S3Writer[E, s3.S3 with E with S3Config, Array[Byte]] {
  override def apply(stream: ZStream[E, Throwable, Array[Byte]]): ZIO[S3 with E with S3Config, Throwable, Unit] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      _ <- multipartUpload(
        bucket,
        location,
        stream.flatMap(r => ZStream.fromIterable(r)),
        MultipartUploadOptions.fromUploadOptions(UploadOptions.fromContentType("application/zip"))
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield ()

}