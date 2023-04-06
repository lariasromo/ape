package com.libertexgroup.ape.pipelines

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.{ClickhouseContainerService, KafkaContainerService, KafkaUtils}
import com.libertexgroup.configs.{KafkaConfig, MultiClickhouseConfig}
import com.libertexgroup.models.clickhouse.ClickhouseModel
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.testcontainers.containers.ClickHouseContainer
import zio.kafka.consumer.Consumer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

object PipelineTest extends ZIOSpec[KafkaContainer with KafkaConfig with Consumer with MultiClickhouseConfig with ClickHouseContainer] {
  val pp = Ape.readers.kafka.default.**[dummy] --> (
      Ape.writers.misc.console[Any, Consumer, dummy] ++ Ape.writers.clickhouse.default[Consumer, dummy]
    )
    
  override def spec: Spec[KafkaContainer with KafkaConfig with Consumer with MultiClickhouseConfig with ClickHouseContainer with TestEnvironment with Scope, Any] =
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

  override def bootstrap: ZLayer[Any, Throwable, KafkaContainer with KafkaConfig with Consumer with MultiClickhouseConfig with ClickHouseContainer] =
    KafkaContainerService.topicLayer("pipe_topic") >+> KafkaUtils.consumerLayer ++ ClickhouseContainerService.layer
}
