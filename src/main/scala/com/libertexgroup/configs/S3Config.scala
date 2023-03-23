package com.libertexgroup.configs

import com.libertexgroup.models.CompressionType
import com.libertexgroup.models.CompressionType.CompressionType
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import zio.System.{env, envOrElse}
import zio.s3.errors.ConnectionError
import zio.s3.providers.default
import zio.s3.{Live, S3, liveZIO}
import zio.{Duration, Scope, Task, ZIO, ZLayer}

import scala.util.Try

case class S3Config (
                      location: Option[String],
                      s3Bucket: Option[String],
                      s3Host: String,
                      compressionType: CompressionType,
                      parallelism: Int,
                      enableBackPressure: Boolean,
                      fileCacheExpiration: Option[zio.Duration],
                      filePeekDuration: Option[zio.Duration],
                      filePeekDurationMargin: Option[zio.Duration]
  ) {
  val taskLocation: Task[String] = ZIO.getOrFail(location)
  val taskS3Bucket: Task[String] = ZIO.getOrFail(s3Bucket)
}

object S3Config extends ReaderConfig {
  def make: ZIO[Any, SecurityException, S3Config] = for {
    location <- env("S3_LOCATION")
    parallelism <- envOrElse("S3_PARALLELISM", "4")
    fileCacheExpiration <- envOrElse("S3_FILE_CACHE_EXPIRATION", "PT1H")
    filePeekDuration <- envOrElse("S3_FILE_PEEK_DURATION", "PT1H")
    filePeekDurationMargin <- envOrElse("S3_FILE_PEEK_DURATION_MARGIN", "PT1H")
    s3Bucket <- env("S3_BUCKET")
    compressionType <- envOrElse("COMPRESSION_TYPE", "GZIP")
    s3Host <- envOrElse("S3_OVERRIDE_URL", "https://s3.eu-west-1.amazonaws.com")
    enableBackPressure <- envOrElse("S3_BACK_PRESSURE", "false")
  } yield S3Config (
    location = location,
    s3Bucket = s3Bucket,
    s3Host = s3Host,
    compressionType = CompressionType.withName(compressionType),
    parallelism = Try(parallelism.toInt).toOption.getOrElse(4),
    enableBackPressure = enableBackPressure.equalsIgnoreCase("true"),
    fileCacheExpiration = Some(Duration fromJava java.time.Duration.parse(fileCacheExpiration)),
    filePeekDuration = Some(Duration fromJava java.time.Duration.parse(filePeekDuration)),
    filePeekDurationMargin = Some(Duration fromJava java.time.Duration.parse(filePeekDurationMargin)),
  )

  val makeS3Default: ZIO[Scope, Throwable, Live] =
    for {
      reg <- envOrElse("AWS_REGION", "eu-west-1")
      service <- ZIO.fromAutoCloseable(ZIO.attempt(
        S3AsyncClient
          .builder()
          .credentialsProvider(DefaultCredentialsProvider.create())
          .region(Region.of(reg)).build()))
        .mapBoth(e => ConnectionError(e.getMessage, e.getCause), new zio.s3.Live(_))
    } yield service

  def liveS3Default: ZLayer[Any, Throwable, S3] = ZLayer.scoped {
    makeS3Default
  }

  val live: ZLayer[Any, Throwable, S3Config] = ZLayer.fromZIO(make)
}
