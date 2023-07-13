package ape.kafka.readers

import ape.kafka.configs.KafkaConfig
import ape.kafka.utils.KafkaContainerService
import com.dimafeng.testcontainers.KafkaContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object KafkaTextReaderTest extends ZIOSpec[KafkaConfig with KafkaContainer] {
  val reader = ape.kafka.Readers.readersFlattened[KafkaConfig].string

  override def spec: Spec[KafkaConfig with KafkaContainer with TestEnvironment with Scope, Any] =
    suite("KafkaReaderTest")(
      test("Reads plaintext message") {
        for {
          _ <- ZIO.logInfo("Sending text message")
          _ <- KafkaContainerService.sendPlaintextMessage
          stream <- reader.apply
          data <- stream
            .tap(d => ZIO.logInfo(d.value()))
            .runHead
        } yield {
          assertTrue(data.nonEmpty)
          val (k, v) = (data.orNull.key(), data.orNull.value())
          assertTrue(k.equals("Some key"))
          assertTrue(v.equals("Some value"))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer] =
    KafkaContainerService.topicLayer("string_topic")
}
