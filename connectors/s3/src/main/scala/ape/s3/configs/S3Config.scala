package ape.s3.configs

import ape.s3.models.CompressionType
import ape.s3.models.CompressionType.CompressionType
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryPolicy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import zio.Config.LocalDate
import zio.System.{env, envOrElse}
import zio.s3.errors.ConnectionError
import zio.s3.{Live, S3}
import zio.{Duration, Scope, Task, ZIO, ZLayer}

import java.net.URI
import java.time.{ZoneId, ZonedDateTime}
import scala.annotation.meta.param
import scala.util.Try

case class S3Config (
                    @param
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
                      startDate: Option[ZonedDateTime]=None,
                      fileName:Option[String]=None,
                      filePrefix:Option[String]=None,
                      fileSuffix:Option[String]=None,
                      chunkSizeMb:Option[Int]=None,
  ) {
  val chunkSizeKb = chunkSizeMb.map(mb => mb * 1024L * 1024L)
  val taskLocation: Task[String] = ZIO.succeed{
    location.getOrElse({
      locationPattern.map(l => l(ZonedDateTime.now())).getOrElse("location and location pattern are empty")
    })
  }
  val taskS3Bucket: Task[String] = ZIO.succeed(s3Bucket.getOrElse(throw new Exception("No bucket found in S3Config")))

  def s3: ZIO[Scope, Throwable, Live] =
    for {
      service <- ZIO.fromAutoCloseable(
        ZIO.attempt({
            val builder = S3AsyncClient
              .builder()
              .credentialsProvider(DefaultCredentialsProvider.create())
              .region(Region.of(region))
              .overrideConfiguration(
                ClientOverrideConfiguration
                  .builder()
                  .retryPolicy(
                    RetryPolicy
                      .builder()
                      .numRetries(10)
                      .build()
                  )
                  .build()
              )
            s3Host.map(h => builder.endpointOverride(URI.create(h)))
            builder.build()
        }))
        .mapBoth(
          e => ConnectionError(e.getMessage, e.getCause),
          c => new zio.s3.Live(c)
        )
    } yield service

  def liveS3: ZLayer[Any, Throwable, S3] = ZLayer.scoped { s3 }
}

object S3Config {
  def make(prefix:Option[String]=None): ZIO[Any, SecurityException, S3Config] = for {
    location <- env(prefix.map(s=>s+"_").getOrElse("") + "S3_LOCATION")
    parallelism <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "S3_PARALLELISM", "4")
    fileCacheExpiration <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "S3_FILE_CACHE_EXPIRATION", "PT1H")
    filePeekDuration <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "S3_FILE_PEEK_DURATION", "PT1H")
    filePeekDurationMargin <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "S3_FILE_PEEK_DURATION_MARGIN", "PT1H")
    s3Bucket <- env(prefix.map(s=>s+"_").getOrElse("") + "S3_BUCKET")
    compressionType <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "S3_COMPRESSION_TYPE", "GZIP")
    s3Host <- env(prefix.map(s=>s+"_").getOrElse("") + "S3_OVERRIDE_URL")
    startDate <- env(prefix.map(s=>s+"_").getOrElse("") + "S3_START_DATE")
    reg <- envOrElse(prefix.map(s=>s+"_").getOrElse("") +  "S3_AWS_REGION", "eu-west-1")
    fileName <- env(prefix.map(s=>s+"_").getOrElse("") +  "S3_FILE_NAME")
    fileSuffix <- env(prefix.map(s=>s+"_").getOrElse("") +  "S3_FILE_SUFFIX")
    filePrefix <- env(prefix.map(s=>s+"_").getOrElse("") +  "S3_FILE_PREFIX")
    chunkSizeMb <- env(prefix.map(s=>s+"_").getOrElse("") +  "S3_CHUNK_SIZE_MB")
  } yield S3Config (
    region = reg,
    location = location,
    s3Bucket = s3Bucket,
    s3Host = s3Host,
    compressionType = CompressionType.withName(compressionType),
    parallelism = Try(parallelism.toInt).toOption.getOrElse(4),
    fileCacheExpiration = Some(Duration fromJava java.time.Duration.parse(fileCacheExpiration)),
    filePeekDuration = Some(Duration fromJava java.time.Duration.parse(filePeekDuration)),
    filePeekDurationMargin = Some(Duration fromJava java.time.Duration.parse(filePeekDurationMargin)),
    startDate = startDate.flatMap(sd => {
      LocalDate.parse(sd).map(dt => dt.atStartOfDay(ZoneId.of("UTC"))).toOption
    }),
    fileName=fileName,
    fileSuffix=fileSuffix,
    filePrefix=filePrefix,
    chunkSizeMb=chunkSizeMb.flatMap(m => Try(m.toInt).toOption),
  )

  def makeWithPattern(pattern:ZonedDateTime=>String, prefix:Option[String]=None): ZIO[Any, SecurityException, S3Config] = for {
    conf <- make(prefix)
  } yield conf.copy(locationPattern = Some(pattern), location = None)


  def live(prefix:Option[String]=None): ZLayer[Any, Throwable, S3Config] = ZLayer.fromZIO(make(prefix))

  def liveWithPattern(pattern:ZonedDateTime=>String, prefix:Option[String]=None): ZLayer[Any, SecurityException, S3Config] =
    ZLayer.fromZIO(makeWithPattern(pattern, prefix))
}
