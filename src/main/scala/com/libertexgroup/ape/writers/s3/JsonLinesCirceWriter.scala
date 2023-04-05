package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import zio.ZIO
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[writers] class JsonLinesCirceWriter[E, T: Encoder : ClassTag] extends S3Writer[E with S3 with S3Config, E, T, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E with S3 with S3Config, Throwable, ZStream[E, Throwable, T]] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      bytesStream = stream
        .map(s => s.asJson.noSpaces + "\n")
        .map(_.getBytes)
        .flatMap(bytes => ZStream.fromIterable(bytes))
      fileName <- zio.Random.nextUUID
      _ <- multipartUpload(
        bucket,
        s"${location}/${fileName}.json",
        bytesStream,
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield stream
}