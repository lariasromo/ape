package ape.s3.readers

import ape.s3.configs.S3Config
import ape.s3.utils.S3Utils
import zio.Clock.currentDateTime
import zio.Console.printLine
import zio.concurrent.ConcurrentMap
import zio.s3.{ListObjectOptions, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Schedule, Tag, ZIO}

import java.security.MessageDigest
import java.time.{OffsetDateTime, ZonedDateTime}

protected [s3] class FileReaderContinuous[Config <: S3Config :Tag]
  (locationPattern:ZIO[Config, Throwable, ZonedDateTime => List[String]]) extends S3FileReader[Config] {
  val md5: String => Array[Byte] = s => MessageDigest.getInstance("MD5").digest(s.getBytes)

  def createStreamOfFiles(stream: ZStream[Any, Throwable, OffsetDateTime]):
    ZIO[Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] =
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
            objs <- listObjects(bucket, ListObjectOptions.from(location, 100)).provideLayer(config.liveS3)
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

  override protected[this] def read: ZIO[Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] = for {
    config <- ZIO.service[Config]
    _ <- printLine("Starting s3 files stream reader with periodicity of: " + config.filePeekDuration.orNull)
    now <- currentDateTime
    pastDatesStream <- config.startDate.map(sd => for {
      _ <- printLine("S3 start date is defined, so it will read files from: " + sd.toString)
    } yield ZStream
        .fromIterable(S3Utils.dateRange(sd, now.toZonedDateTime, config.filePeekDuration.orNull))
        .map(_.toOffsetDateTime)
    ).getOrElse(ZIO.succeed(ZStream.empty))
    newFilesStream = ZStream
      .fromSchedule(Schedule.spaced(config.filePeekDuration.orNull))
      .mapZIO(_ => currentDateTime)
    stream <- createStreamOfFiles(pastDatesStream ++ newFilesStream)
  } yield stream
}

