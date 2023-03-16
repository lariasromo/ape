package com.libertexgroup.ape.writers.kafka

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.readers.kafka.AvroReader
import com.libertexgroup.ape.utils.{KafkaContainerService, KafkaUtils}
import com.libertexgroup.ape.writers.kafka.KafkaTextWriterTest.{suite, test}
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Chunk, Scope, ZIO, ZLayer}
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}

import java.time.{LocalDateTime, ZoneOffset}

object KafkaAvroWriterTest extends ZIOSpec[KafkaConfig with KafkaContainer with Consumer with Producer] {
  val sampleObjects: Chunk[dummy] = Chunk(
    dummy("string 1", "other string"),
    dummy("string 2", "some other string")
  )

  def data(topicName:String): ZStream[Any, Nothing, ProducerRecord[String, dummy]] = ZStream.fromChunk(sampleObjects)
    .map(s => {
      val ts = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
      new ProducerRecord[String, dummy](
        topicName, 0, ts, "some key", s
      )
    })

  val writer = Pipeline.writers.kafkaAvroWriter[Any, dummy]
  val reader = Pipeline.readers.kafkaAvroReader[dummy]

  override def spec: Spec[KafkaConfig with KafkaContainer with Consumer with Producer with TestEnvironment with Scope, Any] =
    suite("KafkaAvroWriterTest")(
      test("Writes avro messages"){
        for {
          config <- ZIO.service[KafkaConfig]
          _ <- zio.Console.printLine("Sending dummy message")
          _ <- writer.apply(data(config.topicName))
          stream <- reader.apply
          data <- stream.map(_.value()).runHead
        } yield {
          val result = data.flatten
          assertTrue(result.nonEmpty)
          assertTrue(result.orNull.equals(dummy("string 1", "other string")))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer with Consumer with Producer] =
    KafkaContainerService.topicLayer("text_topic") >+> (KafkaUtils.producerLayer ++ KafkaUtils.consumerLayer)
}

