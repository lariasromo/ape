package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import zio.ZIO
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

protected[writers] class TextWriter[E]
  extends S3Writer[E with S3 with S3Config, E, String, String] {
  override def apply(stream: ZStream[E, Throwable, String]):
  ZIO[E with S3 with S3Config, Throwable, ZStream[E, Throwable, String]] =
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
    } yield stream
}