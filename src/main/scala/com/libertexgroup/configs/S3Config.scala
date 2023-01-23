package com.libertexgroup.configs

import com.libertexgroup.models.EncodingType
import com.libertexgroup.models.EncodingType.EncodingType
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.s3.providers.const
import zio.s3.{ConnectionError, InvalidSettings, Live, S3, S3Region}
import zio.{Has, Task, ZIO, ZLayer, ZManaged, system}

import scala.util.Try

case class S3Config (
                      location: Option[String],
                      s3Bucket: Option[String],
                      s3Host: String,
                      encodingType: EncodingType,
                      parallelism: Int,
                      awsAccessKey: String,
                      awsSecretKey: String,
  ) {


  val taskLocation: Task[String] = ZIO.getOrFail(location)
  val taskS3Bucket: Task[String] = ZIO.getOrFail(s3Bucket)
}

object S3Config extends ReaderConfig {
  def make: ZIO[system.System, SecurityException, S3Config] = for {
    awsAccessKey <- system.envOrElse("AWS_ACCESS_KEY", "")
    awsSecretKey <- system.envOrElse("AWS_SECRET_KEY", "")
    location <- system.env("S3_LOCATION")
    parallelism <- system.envOrElse("S3_PARALLELISM", "4")
    s3Bucket <- system.env("S3_BUCKET")
    encodingType <- system.envOrElse("ENCODING_TYPE", "GZIP")
    s3Host <- system.envOrElse("S3_OVERRIDE_URL", "https://s3.eu-west-1.amazonaws.com")
  } yield S3Config (
    awsAccessKey = awsAccessKey,
    awsSecretKey = awsSecretKey,
    location = location,
    s3Bucket = s3Bucket,
    s3Host = s3Host,
    encodingType = EncodingType.withName(encodingType),
    parallelism = Try(parallelism.toInt).toOption.getOrElse(4)
  )

  val liveFromS3Config: ZLayer[Has[S3Config], S3Exception, Has[S3.Service]] =
    ZLayer.fromManaged{
      ZManaged.fromEffect(
        for {
          config <- ZIO.access[Has[S3Config]](_.get)
          region <- ZIO.fromEither(S3Region.from(Region.EU_WEST_1))
        } yield (config, region)
      ).flatMap {
        case (config: S3Config, region: S3Region) =>
          zio.s3.Live.connect(region, const(config.awsAccessKey, config.awsSecretKey), None)
      }
    }
  val live: ZLayer[system.System, SecurityException, Has[S3Config]] = ZLayer.fromEffect(make)
}
