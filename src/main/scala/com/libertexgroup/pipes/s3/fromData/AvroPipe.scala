package com.libertexgroup.pipes.s3.fromData

import com.libertexgroup.configs.S3Config
import com.libertexgroup.utils.AvroUtils.implicits._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{MultipartUploadOptions, S3, UploadOptions, multipartUpload}
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag
protected[s3] class AvroPipe[E,
  T >:Null :SchemaFor :Decoder :Encoder : ClassTag,
  Config <: S3Config :Tag
] extends S3Pipe[E with S3 with Config, E, T, T] {

  override protected[this] def pipe(i: ZStream[E, Throwable, T]):
    ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, T]] = for {
    config <- ZIO.service[Config]
    bucket <- config.taskS3Bucket
    location <- config.taskLocation
    fileName <- zio.Random.nextUUID
    _ <- multipartUpload(
      bucket,
      s"${location}/${fileName}",
        i.map(r => {
          r.encode().orNull
        })
        .flatMap(r => ZStream.fromIterable(r)),
      MultipartUploadOptions.fromUploadOptions(UploadOptions.fromContentType("application/zip"))
    )(config.parallelism)
      .catchAll(_ => ZIO.unit)
  } yield i
}