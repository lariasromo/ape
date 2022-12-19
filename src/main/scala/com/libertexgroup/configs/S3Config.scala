package com.libertexgroup.configs

import com.libertexgroup.models.EncodingType
import com.libertexgroup.models.EncodingType.EncodingType
import zio.{Task, ZIO, system}

case class S3Config (
                      location:Option[String],
                      s3Bucket:Option[String],
                      s3Host:String,
                      encodingType:EncodingType,
                      parallelism: Int
  )
  {


  val taskLocation: Task[String] = ZIO.getOrFail(location)
  val taskS3Bucket: Task[String] = ZIO.getOrFail(s3Bucket)
}

object S3Config extends ReaderConfig {
  def make: ZIO[system.System, SecurityException, S3Config] = for {
    location <- system.env("S3_LOCATION")
    parallelism <- system.env("S3_PARALLELISM")
    s3Bucket <- system.env("S3_BUCKET")
    encodingType <- system.env("ENCODING_TYPE")
    s3Host <- system.envOrElse("S3_OVERRIDE_URL", "https://s3.eu-west-1.amazonaws.com")
  } yield S3Config (
    location = location,
    s3Bucket = s3Bucket,
    s3Host = s3Host,
    encodingType = encodingType.map(t => EncodingType.withName(t)).getOrElse(EncodingType.GZIP),
    parallelism = parallelism.map(_.toInt).getOrElse(4)
  )
}
