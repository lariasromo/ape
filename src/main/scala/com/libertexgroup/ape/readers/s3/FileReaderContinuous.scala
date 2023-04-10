package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader
import com.libertexgroup.configs.S3Config
import zio.Clock.currentDateTime
import zio.Console.printLine
import zio.concurrent.ConcurrentMap
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Schedule, Tag, ZIO}

import java.security.MessageDigest
import java.time.ZonedDateTime

protected [s3] class FileReaderContinuous[Config <: S3Config :Tag, AWSS3 <: S3]
(locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]])
  extends Reader[Config, AWSS3, S3ObjectSummary] {
  val md5: String => Array[Byte] = s => MessageDigest.getInstance("MD5").digest(s.getBytes)

  override def apply: ZIO[Config, Throwable, ZStream[AWSS3, Throwable, S3ObjectSummary]] = for {
    config <- ZIO.service[Config]
    locPattern <- locationPattern
    bucket <- config.taskS3Bucket
    trackedFiles <- ConcurrentMap.empty[Array[Byte], ZonedDateTime]
    _ <- printLine("Starting s3 files stream reader with periodicity of: " + config.filePeekDuration.orNull)
  } yield ZStream
    .fromSchedule(Schedule.spaced(config.filePeekDuration.orNull))
    .mapZIO(_ => currentDateTime)
    .tap{now => trackedFiles.removeIf((_, date) => date.toEpochSecond < now.minus(config.fileCacheExpiration.orNull).toEpochSecond)}
    .flatMap(now => {
      val locs = locPattern(now.toZonedDateTime).distinct
      ZStream.fromIterable(locs).map(loc => (now, loc))
    }
    )
    .mapZIO {
      case(now, location) => for {
        objs <- listObjects(bucket, ListObjectOptions.from(location, 100))
      } yield ZStream.fromChunk(objs.objectSummaries)
        .filterZIO {
          summary => for {
            // We want to only work with files that hasn't been previously visited (not in hashmap).
            // Then add the current file to the map
            exists <- trackedFiles.exists ((k, _) => k sameElements md5 (summary.key) )
            _ <- ZIO.when (! exists) (trackedFiles.put (md5 (summary.key), now.toZonedDateTime) )
          } yield ! exists
        }
    }.flatMap{x=>x}
}

