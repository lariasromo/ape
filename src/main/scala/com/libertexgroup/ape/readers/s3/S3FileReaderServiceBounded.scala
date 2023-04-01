package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{Duration, ZIO, ZLayer}

import java.time.ZonedDateTime

class S3FileReaderServiceBounded(override val fileStream: ZStream[S3Config with S3, Throwable, S3ObjectSummary])
  extends S3FileReaderService

object S3FileReaderServiceBounded {
  def make(
            locationPattern:ZIO[S3Config, Nothing, ZonedDateTime => List[String]],
            start:ZonedDateTime,
            end:ZonedDateTime,
            step:Duration
          ): ZIO[S3Config with S3, Throwable, S3FileReaderServiceBounded] = ZIO.scoped {
    for {
      files <- Ape.readers.s3FileReaderBounded(locationPattern, start, end, step).apply
      stream = files.tap { file => printLine(s"Getting file ${file.key} from queue") }
    } yield new S3FileReaderServiceBounded(stream)
  }

  def live(
            locationPattern:ZIO[S3Config, Nothing, ZonedDateTime => List[String]],
            start:ZonedDateTime,
            end:ZonedDateTime,
            step:Duration
          ): ZLayer[S3Config with S3, Throwable, S3FileReaderService] =
    ZLayer.fromZIO(make(locationPattern, start, end, step))
}
