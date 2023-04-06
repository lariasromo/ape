package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import zio.{Tag, ZIO}
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

protected[s3] class TextWriter[E,
  Config <: S3Config :Tag
]
  extends S3Writer[E with S3 with Config, E, String, String] {
  override def apply(stream: ZStream[E, Throwable, String]):
  ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, String]] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      fileName <- zio.Random.nextUUID
      _ <- multipartUpload(
        bucket,
        s"${location}/${fileName}.txt",
        stream.map(s => s"$s\n".getBytes).flatMap(r => ZStream.fromIterable(r)),
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield stream
}