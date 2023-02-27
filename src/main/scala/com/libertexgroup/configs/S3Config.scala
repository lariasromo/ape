package com.libertexgroup.configs

import com.libertexgroup.models.CompressionType
import com.libertexgroup.models.CompressionType.CompressionType
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.System.{env, envOrElse}
import zio.s3.{S3, errors}
import zio.{Layer, Task, ZIO, ZLayer}

import scala.util.Try

case class S3Config (
                      location: Option[String],
                      s3Bucket: Option[String],
                      s3Host: String,
                      compressionType: CompressionType,
                      parallelism: Int,
                      awsAccessKey: String,
                      awsSecretKey: String,
  ) {
  val taskLocation: Task[String] = ZIO.getOrFail(location)
  val taskS3Bucket: Task[String] = ZIO.getOrFail(s3Bucket)
}

object S3Config extends ReaderConfig {
  def make: ZIO[System, SecurityException, S3Config] = for {
    awsAccessKey <- envOrElse("AWS_ACCESS_KEY", "")
    awsSecretKey <- envOrElse("AWS_SECRET_KEY", "")
    location <- env("S3_LOCATION")
    parallelism <- envOrElse("S3_PARALLELISM", "4")
    s3Bucket <- env("S3_BUCKET")
    compressionType <- envOrElse("COMPRESSION_TYPE", "GZIP")
    s3Host <- envOrElse("S3_OVERRIDE_URL", "https://s3.eu-west-1.amazonaws.com")
  } yield S3Config (
    awsAccessKey = awsAccessKey,
    awsSecretKey = awsSecretKey,
    location = location,
    s3Bucket = s3Bucket,
    s3Host = s3Host,
    compressionType = CompressionType.withName(compressionType),
    parallelism = Try(parallelism.toInt).toOption.getOrElse(4)
  )

  val makeFromS3Config: ZIO[S3Config, errors.InvalidSettings, Layer[S3Exception, S3]] =
    for {
      config <- ZIO.service[S3Config]
      region <- ZIO.succeed(Region.EU_WEST_1)
    } yield zio.s3.live(region, AwsBasicCredentials.create(config.awsAccessKey, config.awsSecretKey))


  val liveFromS3Config: ZLayer[S3Config, errors.InvalidSettings, Layer[S3Exception, S3]] = ZLayer.scoped(makeFromS3Config)
  val live: ZLayer[System, SecurityException, S3Config] = ZLayer.fromZIO(make)
}
