package com.libertexgroup.ape.readers.kafka

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.{KafkaContainerService, KafkaUtils}
import com.libertexgroup.configs.KafkaConfig
import zio.kafka.consumer.Consumer
import zio.test.TestAspect.sequential
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object KafkaBytesReaderTest extends ZIOSpec[KafkaConfig with KafkaContainer with Consumer] {
  val reader = Pipeline.readers.kafkaDefaultReader
  override def spec: Spec[KafkaConfig with KafkaContainer with Consumer with TestEnvironment with Scope, Any] =
    suite("KafkaReaderTest")(
      test("Reads bytes"){
        for {
          _ <- zio.Console.printLine("Sending bytes")
          _ <- KafkaContainerService.sendBytes
          stream <- reader.apply
          data <- stream
            .tap(d => zio.Console.printLine(d.value().mkString))
            .runHead
        } yield {
          assertTrue(data.nonEmpty)
          val (k, v) = (data.orNull.key(), data.orNull.value())
          assertTrue(k.equals("Some key"))
          assertTrue(v.sameElements("Some value".getBytes()))
        }
      }
  )

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer with Consumer] =
    KafkaContainerService.topicLayer("bytes_topic") >+> KafkaUtils.consumerLayer
}