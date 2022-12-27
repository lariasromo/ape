package com.libertexgroup.configs

import com.libertexgroup.models.EncodingType
import com.libertexgroup.models.EncodingType.EncodingType
import zio.{Has, Task, ZIO, ZLayer, system}

import scala.util.Try

case class S3Config (
                      location: Option[String],
                      s3Bucket: Option[String],
                      s3Host: String,
                      encodingType: EncodingType,
                      parallelism: Int
  ) {
  val taskLocation: Task[String] = ZIO.getOrFail(location)
  val taskS3Bucket: Task[String] = ZIO.getOrFail(s3Bucket)
}

object S3Config extends ReaderConfig {
  def make: ZIO[system.System, SecurityException, S3Config] = for {
    location <- system.env("S3_LOCATION")
    parallelism <- system.envOrElse("S3_PARALLELISM", "4")
    s3Bucket <- system.env("S3_BUCKET")
    encodingType <- system.envOrElse("ENCODING_TYPE", "GZIP")
    s3Host <- system.envOrElse("S3_OVERRIDE_URL", "https://s3.eu-west-1.amazonaws.com")
  } yield S3Config (
    location = location,
    s3Bucket = s3Bucket,
    s3Host = s3Host,
    encodingType = EncodingType.withName(encodingType),
    parallelism = Try(parallelism.toInt).toOption.getOrElse(4)
  )
  val live: ZLayer[system.System, SecurityException, Has[S3Config]] = ZLayer.fromEffect(make)
}
