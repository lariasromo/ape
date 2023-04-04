package com.libertexgroup.ape.utils

import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.CompressionType.CompressionType
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import zio.Console.printLine
import zio.s3.errors.ConnectionError
import zio.s3.{S3, UploadOptions, createBucket, putObject}
import zio.stream.ZStream
import zio.{Scope, Task, UIO, ZIO, ZLayer}

import scala.reflect.io.File

object MinioContainerService extends TestContainerHelper[MinioContainer] {
  def config(compressionType: CompressionType, location: Option[String]): ZIO[MinioContainer, Nothing, S3Config] = for {
    container <- ZIO.service[MinioContainer]
  } yield S3Config(
    location = location,
    s3Bucket = Some("ape"),
    s3Host = Some(container.getHostAddress.toString),
    compressionType = compressionType,
    parallelism = 5,
    enableBackPressure = true,
    fileCacheExpiration = None, filePeekDuration = None, filePeekDurationMargin = None
  )

  def setup(eff: ZIO[S3 with S3Config, Throwable, Unit]): ZLayer[S3 with S3Config, Throwable, Unit] =
    ZLayer.fromZIO{
    for {
      _ <- createBBucket
      _ <- eff
    } yield ()
  }

  def configLayer(compressionType: CompressionType, location: Option[String]): ZLayer[MinioContainer, Nothing, S3Config] =
    ZLayer.fromZIO(config(compressionType, location))

  override val startContainer: Task[MinioContainer] = ZIO.attemptBlocking {
    val container: MinioContainer = new MinioContainer()
    container.start()
    container
  }

  override val stopContainer: MinioContainer => UIO[Unit] = c => ZIO.succeedBlocking(c.stop())

  def createBBucket: ZIO[S3 with S3Config, Throwable, String] = for {
    s3Config <- ZIO.service[S3Config]
    bucket <- s3Config.taskS3Bucket
    _ <- createBucket(bucket).catchAll(ex => printLine(ex.getMessage))
  } yield bucket

  def loadSampleData: ZIO[S3 with S3Config, Throwable, Unit] =  for {
    bucket <- createBBucket
    _ <- getFileTree(File("src/test/resources/sample_data/s3").jfile)
      .tap(file => for {
        _ <- putObject( bucket,
          file.getPath
              .replace("\\", "/")
              .replace("src/test/resources/sample_data/s3", ""),
          file.length,
          ZStream.fromFileURI(file.toURI),
          UploadOptions.fromContentType("application/json")
        )
      } yield ())
      .runDrain.orDie
  } yield ()

  def connect[R](): ZIO[Any with Scope with MinioContainer, ConnectionError, S3] =
    for {
      container <- ZIO.service[MinioContainer]
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


  val s3Layer: ZLayer[Any, Throwable, S3 with MinioContainer] = layer >+> ZLayer.scoped{connect}
}
