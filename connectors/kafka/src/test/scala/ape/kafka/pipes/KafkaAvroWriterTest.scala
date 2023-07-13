package ape.kafka.pipes

import ape.kafka.configs.KafkaConfig
import ape.kafka.models.dummy
import ape.kafka.utils.KafkaContainerService
import com.dimafeng.testcontainers.KafkaContainer
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
          stream <- ape.kafka.Readers.readersFlattened[KafkaConfig].avro[dummy].apply
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
    _ <- ZIO.logInfo("Sending dummy message")
    _ <- ape.kafka.Pipes.pipes[KafkaConfig].avro.of[dummy].write(data(config.topicName))
  } yield ()

  override def bootstrap: ZLayer[Any, Any, KafkaConfig with KafkaContainer] =
    KafkaContainerService.topicLayer("text_topic") >+> ZLayer.fromZIO(setup)
}

