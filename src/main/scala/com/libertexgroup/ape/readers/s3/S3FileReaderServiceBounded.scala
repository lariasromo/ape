package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{Duration, Scope, Tag, ZIO, ZLayer}

import java.time.ZonedDateTime

protected [s3] class S3FileReaderServiceBounded[Config <: S3Config :Tag, AWSS3 <: S3 :Tag]
(override val fileStream: ZStream[S3Config with S3, Throwable, S3ObjectSummary])
  extends S3FileReaderService[Config, AWSS3]

object S3FileReaderServiceBounded {
  def make[Config <: S3Config :Tag, AWSS3 <: S3 :Tag](
            locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
            start:ZonedDateTime,
            end:ZonedDateTime,
            step:Duration
          ): ZIO[AWSS3 with Config, Throwable, S3FileReaderServiceBounded[Config, AWSS3]] =
    for {
      files <- Ape.readers.s3[Config, AWSS3].fileReaderBounded(locationPattern, start, end, step).apply
    } yield new S3FileReaderServiceBounded[Config, AWSS3](files)


  def live[Config <: S3Config :Tag, AWSS3 <: S3 :Tag](
            locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
            start:ZonedDateTime,
            end:ZonedDateTime,
            step:Duration
          ): ZLayer[AWSS3 with Config, Throwable, S3FileReaderServiceBounded[Config, AWSS3]] =
    ZLayer.fromZIO(make(locationPattern, start, end, step))
}
