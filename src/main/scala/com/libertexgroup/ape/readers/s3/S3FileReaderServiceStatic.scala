package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{Tag, ZIO, ZLayer}

import java.time.ZonedDateTime

protected [s3] class S3FileReaderServiceStatic[Config <: S3Config :Tag]
(override val fileStream: ZStream[S3Config with S3, Throwable, S3ObjectSummary])
  extends S3FileReaderService[Config]

object S3FileReaderServiceStatic {
  def make[Config <: S3Config :Tag](location: String):
  ZIO[Config with S3, Throwable, S3FileReaderServiceStatic[Config]] = for {
      files <- Ape.readers.s3[Config].fileReaderSimple(ZIO.succeed(_ => List(location))).apply
      stream = files
    } yield new S3FileReaderServiceStatic[Config](stream)

  def live[Config <: S3Config :Tag](location: String):
  ZLayer[Config with S3, Throwable, S3FileReaderServiceStatic[Config]] =
    ZLayer.fromZIO(make[Config](location))
}
