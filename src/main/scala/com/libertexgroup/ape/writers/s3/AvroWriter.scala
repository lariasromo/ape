package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.{Tag, ZIO}
import zio.s3.{MultipartUploadOptions, S3, UploadOptions, multipartUpload}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[s3] class AvroWriter[
  E, T >:Null :SchemaFor :Decoder :Encoder : ClassTag,
  Config <: S3Config :Tag
] extends S3Writer[E with S3 with Config, E, T, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, T]] =
    for {
      config <- ZIO.service[Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      fileName <- zio.Random.nextUUID
      _ <- multipartUpload(
        bucket,
        s"${location}/${fileName}",
        stream
          .map(r => {
            import com.libertexgroup.ape.utils.AvroUtils.implicits._
            r.encode().orNull
          })
          .flatMap(r => ZStream.fromIterable(r)),
        MultipartUploadOptions.fromUploadOptions(UploadOptions.fromContentType("application/zip"))
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield stream
}