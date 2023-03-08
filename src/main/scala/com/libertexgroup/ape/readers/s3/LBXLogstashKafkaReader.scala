package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.KafkaRecordS3
import zio.Clock.currentDateTime
import zio.Console.printLine
import zio.concurrent.ConcurrentMap
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Duration, Schedule, ZIO}

import java.security.MessageDigest
import java.time.ZonedDateTime

class LBXLogstashKafkaReader(locationPattern:ZonedDateTime => List[String], spacedDuration: Duration)
  extends S3Reader[S3Config, S3 with S3Config, (S3ObjectSummary, ZStream[S3, Throwable, KafkaRecordS3])] {

  val md5: String => Array[Byte] = s => MessageDigest.getInstance("MD5").digest(s.getBytes)

  override def apply: ZIO[S3Config, Throwable, ZStream[S3 with S3Config, Throwable,
    (S3ObjectSummary, ZStream[S3, Throwable, KafkaRecordS3])]] = for {
    map <- ConcurrentMap.empty[Array[Byte], String]
    config <- ZIO.service[S3Config]
    bucket <- config.taskS3Bucket
  } yield {
    ZStream
      .fromSchedule(Schedule.spaced(spacedDuration))
      .mapZIO(_ => currentDateTime)
      .flatMap(now => ZStream.fromIterable(locationPattern(now.toZonedDateTime)))
      // we use a concurrent map to keep track of visited s3 paths, every run will get a new set of files in addition
      // of previously explored files since they all shared the same key (divided by hour)
      // If we find files in the map with a pat different than the one we are currently visiting, it is safe to
      // delete other paths to keep the map optimal
      .tap{location => map.removeIf((_, v) => !v.equals(location))}
      .mapZIO{ location => for {
          objs <- listObjects(bucket, ListObjectOptions.from(location, 100))
        } yield objs.objectSummaries.map(e => (location, e))
      }
      .flatMap(s => ZStream.fromChunk(s))
      // We want to only work with files that hasn't been previously visited (not in hashmap).
      // Then add the current file to the map
      .filterZIO {
        case (location, summary) => for {
          exists <- map.exists((k, _) => k sameElements md5(summary.key))
          _ <- ZIO.when(!exists)(map.put(md5(summary.key), location))
        } yield !exists
      }
      .map { case (_, summary) => summary }
      .tap{file => printLine(s"Got file: ${file.key}") }
      .mapZIO(file => for {
        s <- new JsonLinesReader[KafkaRecordS3](file.key).apply
      } yield (file, s))
  }
}
