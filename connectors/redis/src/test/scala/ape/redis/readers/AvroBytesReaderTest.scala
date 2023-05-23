package ape.redis.readers

import ape.redis.configs.RedisConfig
import ape.redis.models.dummy
import ape.redis.utils.RedisContainerService
import com.redis.testcontainers.RedisContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object AvroBytesReaderTest extends ZIOSpec[RedisContainer with RedisConfig]{

  def setup: ZIO[RedisConfig, Throwable, Unit] = for {
    _ <- (dummyReader --> ape.redis.Pipes.pipes[RedisConfig].generalPurpose.typed("someQueue")).runDrain
  } yield ()

  override def spec: Spec[RedisContainer with RedisConfig with TestEnvironment with Scope, Any] =
    suite("AvroBytesReaderTest")(
      test("Reads avro message"){
        for {
          msgs <- (
            ape.redis.Readers.readers[RedisConfig].default[dummy]("someQueue") -->
              ape.misc.Pipes.pipes.console.of[dummy]
            ).take(dummyData.length).runCollect
        } yield {
          assertTrue(msgs.map(_.toString).toList.sorted equals dummyData.map(_.toString).toList.sorted)
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, RedisContainer with RedisConfig] =
    RedisContainerService.live >+> ZLayer.fromZIO(setup)
}
