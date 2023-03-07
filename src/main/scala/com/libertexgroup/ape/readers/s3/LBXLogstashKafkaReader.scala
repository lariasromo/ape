package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.KafkaRecordS3
import zio.Clock.currentDateTime
import zio.Console.printLine
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary, listObjects}
import zio.stream.ZStream
import zio.{Duration, Schedule, ZIO}

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

class LBXLogstashKafkaReader(locationPattern:ZonedDateTime => List[String], spacedDuration: Duration)
  extends S3Reader[S3Config, S3 with S3Config, KafkaRecordS3] {

  def getFilesFromDate(now:ZonedDateTime,
                       location:List[String]): ZIO[S3 with S3Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      objs <- ZIO.foreach(location)(dt =>
        listObjects(bucket, ListObjectOptions.from(dt, 100))
      )
      stream = ZStream
        .fromIterable(objs.flatMap(_.objectSummaries))
        .map(f => {
          val lm: ZonedDateTime = f.lastModified.atZone(ZoneId.of("UTC"))
          (f, f.lastModified.atZone(ZoneId.of("UTC")), now)
        })
        .tap{ case(file, date, now) => printLine(s"key: ${file.key} " +
          s"lastModified: ${date.toEpochSecond}, " +
          s"now: ${now.toEpochSecond}")
        }.filter{ case(_, date, now) => (now.minus(spacedDuration)).toEpochSecond <= date.toEpochSecond }
        .map{ case(file, _, _) => file }
  } yield stream

  override def apply: ZIO[S3Config, Throwable, ZStream[S3 with S3Config, Throwable, KafkaRecordS3]] = ZIO.succeed {
    ZStream
      .fromSchedule(Schedule.spaced(spacedDuration))
      .mapZIO(_ => currentDateTime)
      .map(now => now.toZonedDateTime)
      .map(now => (now,locationPattern(now)))
      .mapZIO{case(now, location) => getFilesFromDate(now, location)}
      .flatMap(s => s)
      .mapZIO(l => new JsonLinesReader[KafkaRecordS3](l.key).apply)
      .flatMap(s => s)
  }
}
