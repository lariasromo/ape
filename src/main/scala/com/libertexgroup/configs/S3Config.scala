package com.libertexgroup.configs

import com.libertexgroup.models.CompressionType
import com.libertexgroup.models.CompressionType.CompressionType
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.System.{env, envOrElse}
import zio.s3.errors.ConnectionError
import zio.s3.{Live, S3, errors}
import zio.{Layer, Scope, Task, ZIO, ZLayer}

import scala.util.Try

case class S3Config (
                      location: Option[String],
                      s3Bucket: Option[String],
                      s3Host: String,
                      compressionType: CompressionType,
                      parallelism: Int,
                      enableBackPressure: Boolean,
                      awsAccessKey: String,
                      awsSecretKey: String,
  ) {
  val taskLocation: Task[String] = ZIO.getOrFail(location)
  val taskS3Bucket: Task[String] = ZIO.getOrFail(s3Bucket)
}

object S3Config extends ReaderConfig {
  def make: ZIO[Any, SecurityException, S3Config] = for {
    awsAccessKey <- envOrElse("AWS_ACCESS_KEY", "")
    awsSecretKey <- envOrElse("AWS_SECRET_KEY", "")
    location <- env("S3_LOCATION")
    parallelism <- envOrElse("S3_PARALLELISM", "4")
    s3Bucket <- env("S3_BUCKET")
    compressionType <- envOrElse("COMPRESSION_TYPE", "GZIP")
    s3Host <- envOrElse("S3_OVERRIDE_URL", "https://s3.eu-west-1.amazonaws.com")
    enableBackPressure <- envOrElse("S3_BACK_PRESSURE", "false")
  } yield S3Config (
    awsAccessKey = awsAccessKey,
    awsSecretKey = awsSecretKey,
    location = location,
    s3Bucket = s3Bucket,
    s3Host = s3Host,
    compressionType = CompressionType.withName(compressionType),
    parallelism = Try(parallelism.toInt).toOption.getOrElse(4),
    enableBackPressure = enableBackPressure.equalsIgnoreCase("true")
  )

  val makeFromS3Config: ZIO[Any with Scope with S3Config, ConnectionError, Live] =
    for {
      config <- ZIO.service[S3Config]
      region <- ZIO.succeed(Region.EU_WEST_1)
      creds <- zio.s3.providers.basic(config.awsAccessKey, config.awsSecretKey)
      service <- ZIO.fromAutoCloseable(ZIO.attempt(
        S3AsyncClient
        .builder()
        .credentialsProvider(creds)
        .region(region).build()))
        .mapBoth(e => ConnectionError(e.getMessage, e.getCause), new zio.s3.Live(_))
    } yield service


  val liveFromS3Config: ZLayer[S3Config, ConnectionError, S3] = ZLayer.scoped(makeFromS3Config)
  val live: ZLayer[Any, Throwable, S3Config] = ZLayer.fromZIO(make)
}
