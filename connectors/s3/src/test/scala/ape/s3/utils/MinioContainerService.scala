package ape.s3.utils

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType.CompressionType
import ape.s3.models.{S3ConfigTest, dummy}
import ape.s3.utils.MinioContainer.MinioContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import zio.s3.errors.ConnectionError
import zio.s3.{S3, UploadOptions, createBucket, putObject}
import zio.stream.ZStream
import zio.{Chunk, Scope, Task, UIO, ZIO, ZLayer}

import scala.reflect.io.File

object MinioContainerService {
  val sampleRecords: Chunk[dummy] = Chunk(
    dummy("value1", "value2"),
    dummy("value3", "value4"),
    dummy("value5", "value6"),
    dummy("value7", "value8"),
    dummy("value9", "value10"),
  )

  val sampleData: ZStream[Any, Throwable, dummy] = ZStream.fromChunk(sampleRecords)

  def config(compressionType: CompressionType, location: Option[String]): ZIO[MinioContainer, Nothing, S3ConfigTest] = for {
    container <- ZIO.service[MinioContainer]
  } yield new S3ConfigTest(
    container=container,
    location = location,
    region = "eu-west-1",
    s3Bucket = Some("ape"),
    s3Host = Some(container.getHostAddress.toString),
    compressionType = compressionType,
    parallelism = 5,
  )

  def setup(eff: ZIO[S3 with S3ConfigTest, Throwable, Unit]): ZLayer[S3 with S3ConfigTest, Throwable, Unit] =
    ZLayer.fromZIO{
    for {
      _ <- createBBucket
      _ <- eff
    } yield ()
  }

  def configLayer(compressionType: CompressionType, location: Option[String]): ZLayer[MinioContainer, Nothing, S3ConfigTest] =
    ZLayer.fromZIO(config(compressionType, location))

  val startContainer: Task[MinioContainer] = ZIO.attemptBlocking {
    val container: MinioContainer = new MinioContainer()
    container.start()
    container
  }

  val stopContainer: MinioContainer => UIO[Unit] = c => ZIO.succeedBlocking(c.stop())

  def createBBucket: ZIO[S3 with S3Config, Throwable, String] = for {
    s3Config <- ZIO.service[S3Config]
    bucket <- s3Config.taskS3Bucket
    _ <- createBucket(bucket).catchAll(ex => ZIO.logError(ex.getMessage))
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

  val layer: ZLayer[Any, Throwable, MinioContainer] = ZLayer.scoped {
    ZIO.acquireRelease(startContainer)(stopContainer)
  }
  val s3Layer: ZLayer[Any, Throwable, S3 with MinioContainer] = layer >+> ZLayer.scoped{connect}
}
