package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream
import zio.{Tag, ZIO}

protected[s3] class TextPipe[E,
  Config <: S3Config :Tag
]
  extends S3Pipe[E with S3 with Config, E, String, String] {

  override protected[this] def pipe(i: ZStream[E, Throwable, String]):
    ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, String]] =
    for {
      config <- ZIO.service[Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      fileName <- zio.Random.nextUUID
      _ <- multipartUpload(
        bucket,
        s"${location}/${fileName}.txt",
        i.map(s => s"$s\n".getBytes).flatMap(r => ZStream.fromIterable(r)),
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield i
}