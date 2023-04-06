package com.libertexgroup.ape.pipelines

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.{ClickhouseContainerService, KafkaContainerService, KafkaUtils}
import com.libertexgroup.configs.{KafkaConfig, MultiClickhouseConfig}
import org.testcontainers.containers.ClickHouseContainer
import zio.kafka.consumer.Consumer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

object PipelineTest extends ZIOSpec[Consumer with KafkaContainer with KafkaConfig with MultiClickhouseConfig with ClickHouseContainer] {
  val pp: ZIO[KafkaConfig with MultiClickhouseConfig, Throwable, Ape[Consumer, (dummy, Chunk[(dummy, Int)])]] =
    Ape.readers.kafka[KafkaConfig].default.**[dummy] --> (
      Ape.writers.misc.console[Any, Consumer, dummy] ++ Ape.writers.clickhouse[MultiClickhouseConfig].default[Consumer, dummy]
    )
    
  override def spec: Spec[Consumer with KafkaContainer with KafkaConfig with MultiClickhouseConfig with ClickHouseContainer with TestEnvironment with Scope, Any] =
    suite("PipelineTest")(
      test("Simple pipeline with PipelineBuilder"){
        for {
          _ <- KafkaContainerService.sendPlaintextMessage
          pipe <- pp
          _ <- pipe.stream.runHead
        } yield {
          assertTrue(true)
        }
      },
    )

  override def bootstrap: ZLayer[Any, Throwable, Consumer with KafkaContainer with KafkaConfig with
    MultiClickhouseConfig with ClickHouseContainer] =
    KafkaContainerService.topicLayer("pipe_topic") >+> KafkaConfig.liveConsumer ++ ClickhouseContainerService.layer
}
