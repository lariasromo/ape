package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.KafkaRecordS3
import zio.Clock.currentDateTime
import zio.Console.printLine
import zio.concurrent.ConcurrentMap
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Duration, Queue, Schedule, ZIO}

import java.security.MessageDigest
import java.time.ZonedDateTime

class LBXLogstashKafkaReader(locationPattern:ZonedDateTime => List[String], spacedDuration: Duration)
  extends S3Reader[S3 with S3Config, S3 with S3Config, (S3ObjectSummary, ZStream[S3, Throwable, KafkaRecordS3])] {
  val md5: String => Array[Byte] = s => MessageDigest.getInstance("MD5").digest(s.getBytes)

  override def apply: ZIO[S3 with S3Config, Throwable, ZStream[S3 with S3Config, Throwable, (S3ObjectSummary, ZStream[S3, Throwable, KafkaRecordS3])]] =
    for {
      trackedFiles <- ConcurrentMap.empty[Array[Byte], ZonedDateTime]
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      s3FilesQueue <- Queue.unbounded[S3ObjectSummary]
      //We leverage 2 streams in parallel, the first stream lists files from S3 in the background and sends
      // metadata to a queue.
      //The second stream gets constructed from the previous queue, this second stream may have sinks that perform
      // heavy operations. This way this stream won't delay the first stream.
      _ <- ZStream
        .fromSchedule(Schedule.spaced(spacedDuration))
        .mapZIO(_ => currentDateTime)
        // we use a concurrent map to keep track of visited s3 paths, every run will get a new set of files in addition
        // of previously explored files since they all shared the same key (divided by hour)
        // If we find files in the map with a pat different than the one we are currently visiting, it is safe to
        // delete other paths to keep the map optimal
        .tap{now => trackedFiles.removeIf((_, date) => date.toEpochSecond < now.minus(spacedDuration.multipliedBy(3)).toEpochSecond)}
        .flatMap(now => ZStream.fromIterable(locationPattern(now.toZonedDateTime)).map(e => (now, e)))
        .mapZIO{case(now, location) => for {
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
          .tap(f => printLine(s"Offering file: ${f.key} to queue") *> s3FilesQueue.offer(f))
        }
        .flatMap{a=>a}
        .runDrain.fork
    } yield {
      ZStream
        .fromQueue(s3FilesQueue)
        .mapZIO(file => for {
          _ <- printLine(s"Getting file ${file.key} from queue")
          stream <- readPlainText(bucket, file.key)
          decodedStream = stream.map(KafkaRecordS3.decoder)
        } yield (file, decodedStream))
  }
}
