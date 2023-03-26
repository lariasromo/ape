package com.libertexgroup.ape.readers.kafka

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.utils.{KafkaContainerService, KafkaUtils}
import com.libertexgroup.configs.KafkaConfig
import zio.kafka.consumer.Consumer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object KafkaTextReaderTest extends ZIOSpec[KafkaConfig with KafkaContainer with Consumer] {
  val reader = Ape.readers.kafkaStringReader
  override def spec: Spec[KafkaConfig with KafkaContainer with Consumer with TestEnvironment with Scope, Any] =
    suite("KafkaReaderTest")(
      test("Reads plaintext message"){
        for {
          _ <- zio.Console.printLine("Sending text message")
          _ <- KafkaContainerService.sendPlaintextMessage
          stream <- reader.apply
          data <- stream
            .tap(d => {
              zio.Console.printLine(d.value())
            })
            .runHead
        } yield {
          assertTrue(data.nonEmpty)
          val (k, v) = (data.orNull.key(), data.orNull.value())
          assertTrue(k.equals("Some key"))
          assertTrue(v.equals("Some value"))
        }
      },
  )

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer with Consumer] =
    KafkaContainerService.topicLayer("string_topic") >+> KafkaUtils.consumerLayer
}
