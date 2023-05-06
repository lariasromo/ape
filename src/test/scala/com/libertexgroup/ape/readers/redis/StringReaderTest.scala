package com.libertexgroup.ape.readers.redis

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.utils.RedisContainerService
import com.libertexgroup.configs.RedisConfig
import com.redis.testcontainers.RedisContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object StringReaderTest extends ZIOSpec[RedisContainer with RedisConfig]{
  def setup: ZIO[RedisConfig, Throwable, Unit] = for {
    _ <- (stringReader --> Ape.pipes.redis[RedisConfig].generalPurpose.string("someQueue")).runDrain
  } yield ()

  override def spec: Spec[RedisContainer with RedisConfig with TestEnvironment with Scope, Any] =
    suite("StringReaderTest")(
      test("Reads string message"){
        for {
          msgs <- (
            Ape.readers.redis[RedisConfig].string("someQueue") -->
              Ape.pipes.misc.consoleString[Any, Any]
            ).take(stringData.length).runCollect
        } yield {
          assertTrue(msgs.toList.sorted equals stringData.toList.sorted)
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, RedisContainer with RedisConfig] =
    RedisContainerService.live >+> ZLayer.fromZIO(setup)
}
