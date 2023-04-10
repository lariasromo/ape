package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader
import com.libertexgroup.configs.S3Config
import zio.Clock.currentDateTime
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO}

import java.time.ZonedDateTime

protected [s3] class FileReaderSimple[Config <: S3Config :Tag, AWSS3 <: S3]
(locationPattern:ZIO[AWSS3 with Config, Nothing, ZonedDateTime => List[String]])
  extends Reader[AWSS3 with Config, Any, S3ObjectSummary] {
  override def apply: ZIO[AWSS3 with Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] = for {
    config <- ZIO.service[Config]
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
}

