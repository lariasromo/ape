package ape.s3.utils

import ape.s3.configs.S3Config
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.{Chunk, Tag, ZIO}
import zio.s3.{ListObjectOptions, MaxKeys, MultipartUploadOptions, S3, S3ObjectListing, S3ObjectSummary, UploadOptions, getNextObjects, listObjects, multipartUpload}
import zio.stream.ZStream

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

object S3Utils {
  def dateRange(start: ZonedDateTime, end: ZonedDateTime, step: java.time.Duration): Seq[ZonedDateTime] = {
    val e = (end.toEpochSecond - start.toEpochSecond) / step.toSeconds
    (0L to e).map(s => end.minus(step multipliedBy s))
  }

  def pathConverter(path: String): ZIO[S3Config, Nothing, ZonedDateTime => List[String]] = for {
    config <- ZIO.service[S3Config]
  } yield {
    val conv: ZonedDateTime => List[String] = date => {
      val zero: Int => String = i => if (i < 10) s"0$i" else i.toString
      val margin = config.filePeekDurationMargin.getOrElse(zio.Duration.Zero)
      date.minus(margin).toEpochSecond
        .to(date.toEpochSecond)
        .map(s => LocalDateTime.ofEpochSecond(s, 0, ZoneOffset.UTC))
        .map(dt => {
          val location = s"$path" +
            s"/year=${zero(dt.getYear)}" +
            s"/month=${zero(dt.getMonthValue)}" +
            s"/day=${zero(dt.getDayOfMonth)}" +
            s"/hour=${zero(dt.getHour)}"
          location
        })
        .toList
    }
    conv
  }

  def uploadStream[E, Config <: S3Config :Tag](fileName:String, stream: ZStream[E, Throwable, Byte]): ZIO[S3 with E
    with Config, Throwable, S3ObjectSummary] = for {
    config <- ZIO.service[Config]
    bucket <- config.taskS3Bucket
    location <- config.taskLocation
    opts = MultipartUploadOptions.fromUploadOptions(UploadOptions.fromContentType("application/zip"))
    _ <- multipartUpload(bucket, s"${location}/${fileName}", stream, opts)(config.parallelism)
      .catchAll(exception => ZIO.logError(exception.getMessage).unit)
    resultFile <- listObjects(bucket, ListObjectOptions.from(s"${location}/${fileName}", 1))
  } yield resultFile.objectSummaries.head


  def listPaginated(bucket: String, location: String, maxKeys: Long): ZIO[S3, S3Exception, Chunk[S3ObjectSummary]] =
    for {
      l1 <- listObjects(bucket, ListObjectOptions.from(location, maxKeys))
      objs <- ZStream.paginateZIO(l1)(listing => for {
        l2 <- getNextObjects(listing)
      } yield ( listing.objectSummaries ++ l2.objectSummaries, l2.nextContinuationToken.map(_=>l2)) )
        .flatMap(ZStream.fromChunk(_))
        .runCollect
    } yield objs
}
