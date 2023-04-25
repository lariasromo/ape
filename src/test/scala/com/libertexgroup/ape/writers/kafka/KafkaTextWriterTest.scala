package com.libertexgroup.ape.writers.kafka

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.utils.{KafkaContainerService, KafkaUtils}
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.consumer.Consumer
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

import java.time.{LocalDateTime, ZoneOffset}

object KafkaTextWriterTest extends ZIOSpec[KafkaContainer with KafkaConfig] {
  val sampleStrings: Chunk[String] = Chunk(
    "string 1",
    "other string",
    "lorem ipsum"
  )

  def data(topicName:String): ZStream[Any, Nothing, ProducerRecord[String, String]] = ZStream.fromChunk(sampleStrings)
    .map(s => {
      val ts = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
      new ProducerRecord[String, String](
        topicName, 0, ts, "some key", s
      )
    })

  override def spec: Spec[KafkaContainer with KafkaConfig with TestEnvironment with Scope, Any] =
    suite("KafkaTextWriterTest")(
      test("Writes plaintext messages"){
        for {
          stream <- Ape.readers.kafka[KafkaConfig].string.apply
          data <- stream
            .tap(d => zio.Console.printLine(d.value()))
            .runHead
        } yield {
          assertTrue(data.nonEmpty)
          val (k, v) = (data.orNull.key(), data.orNull.value())
          assertTrue(k.equals("Some key"))
          assertTrue(v.equals("string 1"))
        }
      },
    )

  val setup: ZIO[KafkaConfig, Throwable, Unit] = for {
    config <- ZIO.service[KafkaConfig]
    _ <- zio.Console.printLine("Sending text message")
    _ <- Ape.writers.kafka[KafkaConfig].string.write(data(config.topicName))
  } yield ()

  override def bootstrap: ZLayer[Any, Any, KafkaContainer with KafkaConfig] =
    KafkaContainerService.topicLayer("text_topic") >+> ZLayer.fromZIO(setup)
}
