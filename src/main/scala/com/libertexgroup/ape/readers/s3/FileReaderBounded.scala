package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader
import com.libertexgroup.ape.utils.S3Utils
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Chunk, Duration, ZIO}

import java.security.MessageDigest
import java.time.ZonedDateTime

class FileReaderBounded(
                         locationPattern:ZIO[S3Config, Nothing, ZonedDateTime => List[String]],
                         start:ZonedDateTime,
                         end:ZonedDateTime,
                         step:Duration
                       ) extends Reader[S3 with S3Config, Any, S3ObjectSummary] {
    val md5: String => Array[Byte] = s => MessageDigest.getInstance("MD5").digest(s.getBytes)

  def a: ZIO[S3 with S3Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] = for {
    config <- ZIO.service[S3Config]
    locPattern <- locationPattern
    bucket <- config.taskS3Bucket
    _ <- printLine("Starting s3 files stream reader with periodicity of: " + config.filePeekDuration.orNull)
    c <- Chunk
      .fromIterable(S3Utils.dateRange(start, end, step))
      .flatMap(now => {
        val locs = locPattern(now).distinct
        Chunk.fromIterable(locs)
      })
      .mapZIO(location => for {
        objs <- listObjects(bucket, ListObjectOptions.from(location, 100))
      } yield objs.objectSummaries
      )
  } yield ZStream.fromChunk(c.flatten)

  override def apply: ZIO[S3 with S3Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] = a
}

