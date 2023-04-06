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

protected [s3] class FileReaderBounded[Config <: S3Config, AWSS3 <: S3](
                         locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
                         start:ZonedDateTime,
                         end:ZonedDateTime,
                         step:Duration
                       ) extends Reader[AWSS3 with Config, Any, S3ObjectSummary] {
  val md5: String => Array[Byte] = s => MessageDigest.getInstance("MD5").digest(s.getBytes)

  override def apply: ZIO[AWSS3 with Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] = for {
    config <- ZIO.service[S3Config]
    locPattern <- locationPattern
    bucket <- config.taskS3Bucket
    _ <- printLine(s"Starting s3 bounded files from ${start} to ${end}")
    datesIter = S3Utils.dateRange(start, end, step)
    c <- Chunk
      .fromIterable(datesIter)
      .flatMap(now => {
        val locs = locPattern(now)
        val d = locs.distinct
        Chunk.fromIterable(d)
      })
      .mapZIO(location => for {
        objs <- listObjects(bucket, ListObjectOptions.from(location, 100))
      } yield objs.objectSummaries
      )
  } yield ZStream.fromChunk(c.flatten)
}

