package ape.s3.readers

import ape.s3.configs.S3Config
import ape.s3.utils.S3Utils
import zio.Console.printLine
import zio.s3.{ListObjectOptions, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Chunk, Duration, Tag, ZIO}

import java.security.MessageDigest
import java.time.ZonedDateTime

protected [s3] class FileReaderBounded[Config <: S3Config :Tag](
                         locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
                         start:ZonedDateTime,
                         end:ZonedDateTime,
                         step:Duration
                       ) extends S3FileReader[Config] {
  val md5: String => Array[Byte] = s => MessageDigest.getInstance("MD5").digest(s.getBytes)

  override protected[this] def read: ZIO[Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] = for {
    config <- ZIO.service[Config]
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
        objs <- listObjects(bucket, ListObjectOptions.from(location, 100)).provideLayer(config.liveS3)
      } yield objs.objectSummaries
      )
  } yield ZStream.fromChunk(c.flatten)
}

