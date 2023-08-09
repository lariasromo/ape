package ape.s3.readers

import ape.s3.configs.S3Config
import zio.Clock.currentDateTime
import zio.s3.{ListObjectOptions, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO}

import java.time.ZonedDateTime

protected [s3] class FileReaderSimple[Config <: S3Config :Tag]
(locationPattern:ZIO[Any, Nothing, ZonedDateTime => List[String]]) extends S3FileReader[Config] {

  override protected[this] def read: ZIO[Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] =
    for {
      config <- ZIO.service[Config]
      locPattern <- locationPattern
      bucket <- config.taskS3Bucket
      now <- currentDateTime
      location = locPattern(now.toZonedDateTime)
      objs <- ZIO.foreach(Chunk.fromIterable(location))(l =>
        for {
          loc <- listObjects(bucket, ListObjectOptions.from(l, config.maxKeySize)).provideLayer(config.liveS3)
        } yield loc.objectSummaries
      )

    } yield ZStream.fromChunk(objs.flatten)
}

