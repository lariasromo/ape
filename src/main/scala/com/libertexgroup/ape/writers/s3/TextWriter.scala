package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.utils.ParquetUtils
import com.libertexgroup.configs.S3Config
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream
import zio.{Duration, ZIO}

class TextWriter[E] extends S3Writer[E, S3 with E with S3Config, String] {
  override def apply(stream: ZStream[E, Throwable, String]): ZIO[S3 with E with S3Config, Throwable, Unit] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      _ <- multipartUpload(
        bucket,
        location,
        stream.map(s => s"$s\n".getBytes).flatMap(r => ZStream.fromIterable(r)),
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield ()

}