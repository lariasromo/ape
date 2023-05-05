package com.libertexgroup.ape.readers.redis

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.RedisContainerService
import com.libertexgroup.configs.RedisConfig
import com.redis.testcontainers.RedisContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object AvroBytesReaderTest extends ZIOSpec[RedisContainer with RedisConfig]{

  def setup: ZIO[RedisConfig, Throwable, Unit] = for {
    _ <- (dummyReader --> Ape.writers.redis[RedisConfig].generalPurpose.default("someQueue")).runDrain
  } yield ()

  override def spec: Spec[RedisContainer with RedisConfig with TestEnvironment with Scope, Any] =
    suite("AvroBytesReaderTest")(
      test("Reads avro message"){
        for {
          msgs <- (
            Ape.readers.redis[RedisConfig].default[Any, dummy]("someQueue") -->
              Ape.writers.misc.console[Any, Any, dummy]
            ).take(dummyData.length).runCollect
        } yield {
          assertTrue(msgs.map(_.toString).toList.sorted equals dummyData.map(_.toString).toList.sorted)
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, RedisContainer with RedisConfig] =
    RedisContainerService.live >+> ZLayer.fromZIO(setup)
}