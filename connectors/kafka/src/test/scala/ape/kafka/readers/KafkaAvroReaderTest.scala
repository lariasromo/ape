package ape.kafka.readers

import ape.kafka.configs.KafkaConfig
import ape.kafka.models.dummy
import ape.kafka.utils.KafkaContainerService
import com.dimafeng.testcontainers.KafkaContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object KafkaAvroReaderTest extends ZIOSpec[KafkaConfig with KafkaContainer] {
  val reader = ape.kafka.Readers.readers[KafkaConfig].avro[dummy]

  override def spec: Spec[KafkaConfig with KafkaContainer with TestEnvironment with Scope, Any] =
    suite("KafkaReaderTest")(
      test("Reads avro message") {
        for {
          _ <- ZIO.logInfo("Sending Avro messages")
          _ <- KafkaContainerService.sendAvroMessage
          stream <- reader.apply
          data <- stream.tap(d => {
            val dummy = d.value().get
            ZIO.logInfo(s"a: ${dummy.a} b: ${dummy.b}")
          }).take(1).runCollect
        } yield {
          assertTrue(data.nonEmpty)
          val foundRecord = data
            .filter { record => {
              val (k, v) = (record.key(), record.value())
              k.equals("Some key") && v.contains(dummy("Some", "value"))
            }
            }
          assertTrue(foundRecord.nonEmpty)
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer] =
    KafkaContainerService.topicLayer("avro_topic")
}
