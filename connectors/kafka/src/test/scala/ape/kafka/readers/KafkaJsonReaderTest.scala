package ape.kafka.readers

import ape.kafka.configs.KafkaConfig
import ape.kafka.models.dummy
import ape.kafka.utils.KafkaContainerService
import com.dimafeng.testcontainers.KafkaContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object KafkaJsonReaderTest extends ZIOSpec[KafkaConfig with KafkaContainer] {
  val reader = ape.kafka.Readers.readersFlattened[KafkaConfig].jsonCirce[dummy]

  override def spec: Spec[KafkaConfig with KafkaContainer with TestEnvironment with Scope, Any] =
    suite("KafkaJsonReaderTest")(
      test("Reads plaintext message") {
        for {
          _ <- ZIO.logInfo("Sending text message")
          _ <- KafkaContainerService.sendJsonMessage
          stream <- reader.apply
          data <- stream
            .tap(d => ZIO.logInfo(d.value()))
            .runHead
        } yield {
          assertTrue(data.nonEmpty)
          val (k, v) = (data.orNull.key(), data.orNull.value())
          assertTrue(k.equals("Some key"))
          assertTrue(v.equals(dummy("Some", "value")))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer] =
    KafkaContainerService.topicLayer("string_topic")
}
