package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.KafkaRecordS3
import zio.{Duration, Schedule, ZIO}
import zio.s3.{ListObjectOptions, S3}
import zio.stream.ZStream

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}


class LBXLogstashKafkaReader(locationPattern:LocalDateTime => String, spacedDuration: Duration)
  extends S3Reader[S3Config, S3 with S3Config, KafkaRecordS3] {
  override def apply: ZIO[S3Config, Throwable, ZStream[S3 with S3Config, Throwable, KafkaRecordS3]] = for {
   config <- ZIO.service[S3Config]
   bucket <- config.taskS3Bucket
  } yield ZStream
      .fromSchedule(Schedule.spaced(spacedDuration))
      .map(_ => ZonedDateTime.now())
      .mapZIO(date => {
        for {
          objs <- zio.s3.listObjects(bucket, ListObjectOptions.from(locationPattern(date.toLocalDateTime), 100))
          stream = objs.objectSummaries
            .filter(f => {
              val lm: ZonedDateTime = f.lastModified.atZone(ZoneId.of("UTC"))
              val past = date.minus(spacedDuration)
              lm.toEpochSecond > past.toEpochSecond
            })
        } yield ZStream.fromChunk(stream)
      })
    .flatMap(s => s)
    .mapZIO(l => new JsonLinesReader[KafkaRecordS3](l.key).apply)
    .flatMap(s => s)
}