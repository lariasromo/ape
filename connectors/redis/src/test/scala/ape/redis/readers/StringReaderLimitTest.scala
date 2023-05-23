package ape.redis.readers

import ape.redis.configs.RedisConfig
import ape.redis.utils.RedisContainerService
import com.redis.testcontainers.RedisContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object StringReaderLimitTest extends ZIOSpec[RedisContainer with RedisConfig]{

  def setup: ZIO[RedisConfig, Throwable, Unit] = for {
    _ <- (stringReader --> ape.redis.Pipes.pipes[RedisConfig].generalPurpose.string("someQueue")).runDrain
  } yield ()

  override def spec: Spec[RedisContainer with RedisConfig with TestEnvironment with Scope, Any] =
    suite("StringReaderLimitTest")(
      test("Reads string message limit"){
        for {
          msgs <- (
            ape.redis.Readers.readers[RedisConfig].string("someQueue", stringData.length) -->
              ape.misc.Pipes.pipes.console.ofString
            ).runCollect
        } yield {
          assertTrue(msgs.toList.sorted equals stringData.toList.sorted)
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, RedisContainer with RedisConfig] =
    RedisContainerService.live >+> ZLayer.fromZIO(setup)
}
