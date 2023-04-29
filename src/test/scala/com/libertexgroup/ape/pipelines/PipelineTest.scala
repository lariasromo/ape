package com.libertexgroup.ape.pipelines

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.{ClickhouseContainerService, KafkaContainerService}
import com.libertexgroup.configs.{KafkaConfig, MultiClickhouseConfig}
import org.testcontainers.containers.ClickHouseContainer
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object PipelineTest extends ZIOSpec[KafkaContainer with KafkaConfig with MultiClickhouseConfig with ClickHouseContainer] {
  val pp: ZStream[KafkaConfig with MultiClickhouseConfig with Scope, Throwable, dummy] =
    Ape.readers.kafka[KafkaConfig].default.**[dummy] --> (
      Ape.writers.misc.console[Any, Any, dummy] <*
        Ape.writers.clickhouse[MultiClickhouseConfig].default[Any, dummy]
    )

  override def spec: Spec[KafkaContainer with KafkaConfig with MultiClickhouseConfig with ClickHouseContainer with TestEnvironment with Scope, Any] =
    suite("PipelineTest")(
      test("Simple pipeline with PipelineBuilder"){
        for {
          _ <- KafkaContainerService.sendPlaintextMessage
          result <- pp.take(1).runCollect
        } yield {
          assertTrue(result.equals(dummy("Some key", "Some value")))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Throwable, KafkaContainer with KafkaConfig with MultiClickhouseConfig with ClickHouseContainer] =
    KafkaContainerService.topicLayer("pipe_topic") ++ ClickhouseContainerService.layer
}
