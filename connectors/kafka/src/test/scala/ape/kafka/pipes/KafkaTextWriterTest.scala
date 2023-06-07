package ape.kafka.pipes

import ape.kafka.configs.KafkaConfig
import ape.kafka.utils.KafkaContainerService
import com.dimafeng.testcontainers.KafkaContainer
import org.apache.kafka.clients.producer.ProducerRecord
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
          stream <- ape.kafka.Readers.readers[KafkaConfig].string.apply
          data <- stream
            .tap(d => ZIO.logInfo(d.value()))
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
    _ <- ZIO.logInfo("Sending text message")
    _ <- ape.kafka.Pipes.pipes[KafkaConfig].string.default.write(data(config.topicName))
  } yield ()

  override def bootstrap: ZLayer[Any, Any, KafkaContainer with KafkaConfig] =
    KafkaContainerService.topicLayer("text_topic") >+> ZLayer.fromZIO(setup)
}
