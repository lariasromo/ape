package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader
import com.libertexgroup.configs.S3Config
import zio.Clock.currentDateTime
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Chunk, ZIO}

import java.time.ZonedDateTime

class FileReaderSimple(locationPattern:ZIO[S3 with S3Config, Nothing, ZonedDateTime => List[String]])
  extends Reader[S3 with S3Config, Any, S3ObjectSummary] {
  def a: ZIO[S3 with S3Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] = for {
    config <- ZIO.service[S3Config]
    locPattern <- locationPattern
    bucket <- config.taskS3Bucket
    now <- currentDateTime
    location = locPattern(now.toZonedDateTime)
    objs <- ZIO.foreach(Chunk.fromIterable(location))(l =>
      for {
        loc <- listObjects(bucket, ListObjectOptions.from(l, 100))
      } yield loc.objectSummaries
    )

  } yield ZStream.fromChunk(objs.flatten)

  override def apply: ZIO[S3 with S3Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] = a
}

