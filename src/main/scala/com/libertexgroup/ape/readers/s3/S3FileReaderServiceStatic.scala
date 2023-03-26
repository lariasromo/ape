package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{ZIO, ZLayer}

import java.time.ZonedDateTime

class S3FileReaderServiceStatic (override val fileStream: ZStream[S3Config with S3, Throwable, S3ObjectSummary])
  extends S3FileReaderService

object S3FileReaderServiceStatic {
  val locationPattern: String => ZIO[S3Config, Nothing, ZonedDateTime => List[String]] = location => {
    ZIO.succeed(_ => List(location))
  }

  def make(location: String): ZIO[S3Config with S3, Throwable, S3FileReaderServiceStatic]
  = ZIO.scoped{
    for {
      files <- Ape.readers.s3FileReaderSimple(locationPattern(location)).apply
      stream = files.tap { file => printLine(s"Getting file ${file.key} from queue") }
    } yield new S3FileReaderServiceStatic(stream)
  }

  def live(location: String): ZLayer[S3Config with S3, Throwable, S3FileReaderService] = ZLayer.fromZIO(make(location))
}
