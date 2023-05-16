package com.libertexgroup.ape.pipes.kafka

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.KafkaContainerService
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

import java.time.{LocalDateTime, ZoneOffset}

object KafkaAvroWriterTest extends ZIOSpec[KafkaConfig with KafkaContainer] {
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

  override def spec: Spec[KafkaConfig with KafkaContainer with TestEnvironment with Scope, Any] =
    suite("KafkaAvroWriterTest")(
      test("Writes avro messages"){
        for {
          stream <- Ape.readers.kafka[KafkaConfig].avro[dummy].apply
          data <- stream.map(_.value()).runHead
        } yield {
          val result = data.flatten
          assertTrue(result.nonEmpty)
          assertTrue(result.orNull.equals(dummy("string 1", "other string")))
        }
      },
    )

  val setup: ZIO[KafkaConfig, Throwable, Unit] = for {
    config <- ZIO.service[KafkaConfig]
    _ <- zio.Console.printLine("Sending dummy message")
    _ <- Ape.pipes.kafka[KafkaConfig].avro.of[dummy].write(data(config.topicName))
  } yield ()

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer] =
    KafkaContainerService.topicLayer("text_topic") >+> ZLayer.fromZIO(setup)
}

