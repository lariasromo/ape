package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import zio.{Tag, ZIO}
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[s3] class JsonLinesWriter[E,
  T: ClassTag,
  Config <: S3Config :Tag
](implicit enc: T => String)
 extends S3Writer[E with S3 with Config, E, T, T] {
  def a(stream: ZStream[E, Throwable, T]):
  ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, T]] =
    for {
      config <- ZIO.service[Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      bytesStream = stream
        .map(s => enc(s) + "\n")
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

  override protected[this] def pipe(i: ZStream[E, Throwable, T]):
    ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, T]] = a(i)
}