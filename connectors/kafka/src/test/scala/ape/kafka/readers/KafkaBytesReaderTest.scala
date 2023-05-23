package ape.kafka.readers

import ape.kafka.configs.KafkaConfig
import ape.kafka.utils.KafkaContainerService
import com.dimafeng.testcontainers.KafkaContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object KafkaBytesReaderTest extends ZIOSpec[KafkaConfig with KafkaContainer] {
  val reader = ape.kafka.Readers.readers[KafkaConfig].default

  override def spec: Spec[KafkaConfig with KafkaContainer with TestEnvironment with Scope, Any] =
    suite("KafkaBytesReaderTest")(
      test("Reads bytes") {
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

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer] =
    KafkaContainerService.topicLayer("bytes_topic")
}
