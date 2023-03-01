package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import zio.ZIO
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

protected[writers] class JsonLinesWriter[E, T](implicit enc: T => String) extends S3Writer[E, E with S3 with S3Config, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E with S3 with S3Config, Throwable, Unit] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      bytesStream = stream
        .map(s => enc(s) + "\n")
        .map(_.getBytes)
        .flatMap(bytes => ZStream.fromIterable(bytes))
      _ <- multipartUpload(
        bucket,
        location,
        bytesStream,
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield ()
}