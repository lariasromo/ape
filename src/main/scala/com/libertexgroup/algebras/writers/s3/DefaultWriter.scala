package com.libertexgroup.algebras.writers.s3

import com.libertexgroup.algebras.writers.Writer
import com.libertexgroup.configs.{ClickhouseConfig, S3Config}
import zio.s3.{MultipartUploadOptions, S3, UploadOptions, multipartUpload}
import zio.stream.ZStream
import zio.{Has, ZIO, s3}

class DefaultWriter[E] extends S3Writer[E] {
  override type EnvType = s3.S3 with E with Has[S3Config]
  override type InputType = Array[Byte]

  override def apply(stream: ZStream[E, Throwable, Array[Byte]]): ZIO[S3 with E with Has[S3Config], Any, Unit] =
    for {
      config <- ZIO.access[Has[S3Config]](_.get)
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
