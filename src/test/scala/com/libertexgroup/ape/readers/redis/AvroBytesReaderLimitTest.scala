package com.libertexgroup.ape.readers.redis

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.RedisContainerService
import com.libertexgroup.configs.RedisConfig
import com.redis.testcontainers.RedisContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object AvroBytesReaderLimitTest extends ZIOSpec[RedisContainer with RedisConfig]{


  def setup: ZIO[RedisConfig, Throwable, Unit] = for {
    _ <- (dummyReader --> Ape.pipes.redis[RedisConfig].generalPurpose.typed("someQueue")).runDrain
  } yield ()

  override def spec: Spec[RedisContainer with RedisConfig with TestEnvironment with Scope, Any] =
    suite("AvroBytesReaderLimitTest")(
      test("Reads avro message limit"){
        for {
          msgs <- (
            Ape.readers.redis[RedisConfig].default[dummy]("someQueue", dummyData.length) -->
              Ape.pipes.misc.console.of[dummy]
            ).runCollect
        } yield {
          assertTrue(msgs.map(_.toString).toList.sorted equals dummyData.map(_.toString).toList.sorted)
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, RedisContainer with RedisConfig] =
    RedisContainerService.live >+> ZLayer.fromZIO(setup)
}
