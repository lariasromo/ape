package com.libertexgroup.ape.pipelines

import com.libertexgroup.ape.utils.{KafkaContainerService, KafkaUtils}
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.Consumer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object PipelineTest extends ZIOSpec[KafkaConfig with Consumer] {
  implicit val t: ConsumerRecord[String, Array[Byte]] => String = rec => rec.value().map(_.toChar).mkString

  val pipe = Pipeline.readers.kafkaDefaultReader --> Pipeline.writers.consoleWriter[Consumer, String]

  override def spec: Spec[KafkaConfig with Consumer with TestEnvironment with Scope, Any] = suite("PipelineTest")(
    test("Simple pipeline with PipelineBuilder"){
      for {
        _ <- KafkaContainerService.sendPlaintextMessage
        _ <- pipe.run
      } yield {
        assertTrue(true)
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with Consumer] =
    KafkaContainerService.topicLayer("pipe_topic") >+> KafkaUtils.consumerLayer
}
