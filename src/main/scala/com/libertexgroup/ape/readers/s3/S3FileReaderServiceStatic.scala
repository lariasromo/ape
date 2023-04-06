package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{Tag, ZIO, ZLayer}

import java.time.ZonedDateTime

protected [s3] class S3FileReaderServiceStatic[Config <: S3Config :Tag, AWSS3 <: S3 :Tag]
(override val fileStream: ZStream[S3Config with S3, Throwable, S3ObjectSummary])
  extends S3FileReaderService[Config, AWSS3]

object S3FileReaderServiceStatic {
  def make[Config <: S3Config :Tag, AWSS3 <: S3 :Tag](location: String):
  ZIO[Config with AWSS3, Throwable, S3FileReaderServiceStatic[Config, AWSS3]] = for {
      files <- Ape.readers.s3[Config, AWSS3].fileReaderSimple(ZIO.succeed(_ => List(location))).apply
      stream = files.tap { file => printLine(s"Getting file ${file.key} from queue") }
    } yield new S3FileReaderServiceStatic[Config, AWSS3](stream)

  def live[Config <: S3Config :Tag, AWSS3 <: S3 :Tag](location: String):
  ZLayer[Config with AWSS3, Throwable, S3FileReaderServiceStatic[Config, AWSS3]] =
    ZLayer.fromZIO(make[Config, AWSS3](location))
}
