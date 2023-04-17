package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader
import com.libertexgroup.ape.utils.S3Utils
import com.libertexgroup.configs.S3Config
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.Clock.currentDateTime
import zio.Console.printLine
import zio.concurrent.ConcurrentMap
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Chunk, Schedule, Tag, ZIO}

import java.security.MessageDigest
import java.time.{Instant, OffsetDateTime, ZonedDateTime}

protected [s3] class FileReaderContinuous[Config <: S3Config :Tag, AWSS3 <: S3]
(locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]])
  extends S3FileReader[Config, AWSS3, S3ObjectSummary] {
  val md5: String => Array[Byte] = s => MessageDigest.getInstance("MD5").digest(s.getBytes)

  def createStreamOfFiles(stream: ZStream[Any, Nothing, OffsetDateTime]): ZIO[Config, Throwable, ZStream[S3, S3Exception, S3ObjectSummary]] =
    for {
      config <- ZIO.service[Config]
      locPattern <- locationPattern
      bucket <- config.taskS3Bucket
      trackedFiles <- ConcurrentMap.empty[Array[Byte], ZonedDateTime]
      _ <- printLine("Starting s3 files stream reader with periodicity of: " + config.filePeekDuration.orNull)
    } yield
      stream
        .tap { now => trackedFiles.removeIf((_, date) => date.toEpochSecond < now.minus(config.fileCacheExpiration.orNull).toEpochSecond) }
        .flatMap(now => {
          val locs = locPattern(now.toZonedDateTime).distinct
          ZStream.fromIterable(locs).map(loc => (now, loc))
        })
        .mapZIO {
          case (now, location) => for {
            objs <- listObjects(bucket, ListObjectOptions.from(location, 100))
          } yield ZStream.fromChunk(objs.objectSummaries)
            .filterZIO {
              summary =>
                for {
                  // We want to only work with files that hasn't been previously visited (not in hashmap).
                  // Then add the current file to the map
                  exists <- trackedFiles.exists((k, _) => k sameElements md5(summary.key))
                  _ <- ZIO.when(!exists)(trackedFiles.put(md5(summary.key), now.toZonedDateTime))
                } yield !exists
            }
        }.flatMap { x => x }

  override def apply: ZIO[Config, Throwable, ZStream[AWSS3, Throwable, S3ObjectSummary]] = for {
    config <- ZIO.service[Config]
    _ <- printLine("Starting s3 files stream reader with periodicity of: " + config.filePeekDuration.orNull)
    now <- currentDateTime
    pastDatesStream = config.startDate.map(sd => {
      ZStream
        .fromIterable(S3Utils.dateRange(sd, now.toZonedDateTime, config.filePeekDuration.orNull))
        .map(_.toOffsetDateTime)
    }).getOrElse(ZStream.empty)
    newFilesStream = ZStream
      .fromSchedule(Schedule.spaced(config.filePeekDuration.orNull))
      .mapZIO(_ => currentDateTime)
    stream <- createStreamOfFiles(pastDatesStream ++ newFilesStream)
  } yield stream
}

