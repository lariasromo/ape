package ape.redis.utils

import ape.redis.configs.RedisConfig
import com.redis.testcontainers.RedisContainer
import org.testcontainers.utility.DockerImageName
import zio.{Task, UIO, ZIO, ZLayer, durationInt}


object RedisContainerService {
  val startContainer: Task[RedisContainer] = ZIO.attemptBlocking {
    val container: RedisContainer = new RedisContainer(DockerImageName.parse("redis:7.2-rc1"))
    container.start()
    container
  }
  val stopContainer: RedisContainer => UIO[Unit] = c => ZIO.succeedBlocking(c.stop())

  val containerLayer: ZLayer[Any, Throwable, RedisContainer] =
    ZLayer.scoped { ZIO.acquireRelease(startContainer)(stopContainer) }

  val config: ZIO[RedisContainer, Throwable, RedisConfig] = for {
    container <- ZIO.service[RedisContainer]
  } yield RedisConfig.makeSingleServer(
      container.getHost,
      container.getMappedPort(6379),
      1.minute
    )

  val configLayer: ZLayer[RedisContainer, Throwable, RedisConfig] = ZLayer.fromZIO(config)

  val live: ZLayer[Any, Throwable, RedisContainer with RedisConfig] = containerLayer >+> configLayer
}
