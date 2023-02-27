
package com.libertexgroup.ape.readers.kafka

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.{KafkaContainerService, KafkaUtils}
import com.libertexgroup.configs.KafkaConfig
import zio.kafka.consumer.Consumer
import zio.test.TestAspect.sequential
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object KafkaAvroReaderTest extends ZIOSpec[KafkaConfig with KafkaContainer with Consumer] {
  val reader = Pipeline.readers.kafkaAvroReader[dummy]
  override def spec: Spec[KafkaConfig with KafkaContainer with Consumer with TestEnvironment with Scope, Any] =
    suite("KafkaReaderTest")(
    test("Reads avro message"){
      for {
        _ <- zio.Console.printLine("Sending Avro messages")
        _ <- KafkaContainerService.sendAvroMessage
        stream <- reader.apply
        data   <- stream.tap(d => {
          val dummy = d.value().get
          zio.Console.printLine(s"a: ${dummy.a} b: ${dummy.b}")
        }).take(1).runCollect
      } yield {
        assertTrue(data.nonEmpty)
        val (k, v) = (data.head.key(), data.head.value())
        assertTrue(k.equals("Some key"))
        assertTrue(v.contains(dummy("Some", "value")))
      }
    }
  )

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer with Consumer] =
    KafkaContainerService.topicLayer("avro_topic") >+> KafkaUtils.consumerLayer
}
