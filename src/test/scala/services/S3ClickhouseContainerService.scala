package services

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import com.libertexgroup.configs.{ClickhouseConfig, S3Config}
import com.libertexgroup.models.EncodingType
import org.testcontainers.containers.ContainerState
import org.testcontainers.containers.wait.strategy.Wait
import zio._
import zio.blocking.{Blocking, effectBlocking}

import java.io.File

object S3ClickhouseContainerService {
  type ClickhouseS3 = Has[ContainersAndConfigs]

  case class ContainersAndConfigs(
                                   container: DockerComposeContainer,
                                   chConfig: ClickhouseConfig,
                                   s3Config: S3Config
                                 )

  def clickhouse: ZLayer[Blocking, Nothing, ClickhouseS3] = ZManaged.make {
    effectBlocking {
      val container = DockerComposeContainer(
        new File("src/test/resources/docker-compose/docker-compose.yml"),
        exposedServices = Seq(
          ExposedService("clickhouse1_1", 8123, Wait.forListeningPort),
          ExposedService("minio_1", 9001, Wait.forListeningPort),
        )
      )
      container.start()

      val chContainer: ContainerState = container
        .getContainerByServiceName("clickhouse1_1")
        .getOrElse(throw new Exception("Failed to start clickhouse container"))

      val minioContainer: ContainerState = container
        .getContainerByServiceName("minio_1")
        .getOrElse(throw new Exception("Failed to start minio container"))

      val chConfig = ClickhouseConfig(
        batchSize = 10000,
        host = chContainer.getHost,
        port = chContainer.getMappedPort(8123),
        databaseName = "alexandria",
        username = "username",
        password = "password"
      )

      val s3Config = S3Config(
        location = Some("topics/topicaudience--deduplicated"),
        s3Bucket = Some("ltx-eu-west-1-datalake-bronze"),
        s3Host = "http://minio:9000",
        encodingType = EncodingType.GZIP,
        parallelism = 10
      )
      ContainersAndConfigs(container, chConfig, s3Config)
    }.orDie
  }(containersAndConfigs => {
    effectBlocking(containersAndConfigs.container.stop()).orDie
  }).toLayer

  val layerEffect: ZIO[ClickhouseS3, Nothing, ContainersAndConfigs] =
  (for {
    configs <- ZIO.access[Has[ContainersAndConfigs]](_.get)
  } yield configs)

  val live: ZLayer[ClickhouseS3, Nothing, Has[ClickhouseConfig] with Has[S3Config]] =
    ZLayer.fromEffect(layerEffect.map(_.chConfig)) ++ ZLayer.fromEffect(layerEffect.map(_.s3Config))
}