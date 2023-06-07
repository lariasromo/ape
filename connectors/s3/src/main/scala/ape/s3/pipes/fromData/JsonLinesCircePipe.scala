package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class JsonLinesCircePipe[E,
  T: Encoder : ClassTag,
  Config <: S3Config :Tag
] extends S3Pipe[E with S3 with Config, E, T, T] {
  def a(stream: ZStream[E, Throwable, T]):
  ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, T]] =
    for {
      config <- ZIO.service[Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      bytesStream = stream
        .map(s => s.asJson.noSpaces + "\n")
        .map(_.getBytes)
        .flatMap(bytes => ZStream.fromIterable(bytes))
      randomUUID <- zio.Random.nextUUID
      fileName = config.filePrefix.getOrElse("") + config.fileName.getOrElse(randomUUID) + config.fileSuffix.getOrElse("")
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