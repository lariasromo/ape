package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import zio.{Tag, ZIO}
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[s3] class JsonLinesWriter[E,
  T: ClassTag,
  Config <: S3Config :Tag,
  AWSS3 <: S3 :Tag](implicit enc: T => String)
 extends S3Writer[E with AWSS3 with Config, E, T, T] {
  override def apply(stream: ZStream[E, Throwable, T]):
  ZIO[E with AWSS3 with Config, Throwable, ZStream[E, Throwable, T]] =
    for {
      config <- ZIO.service[S3Config]
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
}