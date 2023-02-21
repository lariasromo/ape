package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{MultipartUploadOptions, S3, UploadOptions, multipartUpload}
import zio.stream.ZStream
import zio.{ZIO, s3}

class AvroWriter[E, T >:Null :SchemaFor :Decoder :Encoder] extends S3Writer[E, S3 with E with S3Config, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[S3 with E with S3Config, Throwable, Unit] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      _ <- multipartUpload(
        bucket,
        location,
        stream
          .map(r => {
            import com.libertexgroup.ape.utils.AvroUtils.implicits._
            r.encode().orNull
          })
          .flatMap(r => ZStream.fromIterable(r)),
        MultipartUploadOptions.fromUploadOptions(UploadOptions.fromContentType("application/zip"))
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield ()

}