package com.libertexgroup.ape.models

import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.CompressionType.CompressionType
import com.libertexgroup.models.s3.CompressionType
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import zio.s3.Live
import zio.s3.errors.ConnectionError
import zio.{Scope, ZIO}

import java.time.ZonedDateTime

class S3ConfigTest(
                    container: MinioContainer,
                    compressionType: CompressionType=CompressionType.NONE,
                    parallelism: Int=1,
                    region: String,
                    location: Option[String]=None,
                    locationPattern: Option[ZonedDateTime=>String]=None,
                    s3Bucket: Option[String]=None,
                    s3Host: Option[String]=None,
                    fileCacheExpiration: Option[zio.Duration]=None,
                    filePeekDuration: Option[zio.Duration]=None,
                    filePeekDurationMargin: Option[zio.Duration]=None,
                    startDate: Option[ZonedDateTime]=None
                  ) extends S3Config(
                              compressionType=compressionType,
                              parallelism=parallelism,
                              region=region,
                              location=location,
                              locationPattern=locationPattern,
                              s3Bucket=s3Bucket,
                              s3Host=s3Host,
                              fileCacheExpiration=fileCacheExpiration,
                              filePeekDuration=filePeekDuration,
                              filePeekDurationMargin=filePeekDurationMargin,
                              startDate=startDate
                  ){
  override def s3: ZIO[Scope, Throwable, Live] =
    for {
      creds <- zio.s3.providers.basic(container.getAwsAccessKey, container.getAwsSecretKey)
      builder <- ZIO.succeed {
        val builder = S3AsyncClient
          .builder()
          .credentialsProvider(creds)
          .region(Region.EU_WEST_1)

        builder.endpointOverride(container.getHostAddress)
        builder
      }
      service <- ZIO.fromAutoCloseable(ZIO.attempt(builder.build()))
        .mapBoth(e => ConnectionError(e.getMessage, e.getCause), new zio.s3.Live(_))
    } yield service

}
