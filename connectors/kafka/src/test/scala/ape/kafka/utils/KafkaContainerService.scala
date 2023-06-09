package ape.kafka.utils

import ape.kafka.configs.KafkaConfig
import ape.kafka.models.dummy
import com.dimafeng.testcontainers.KafkaContainer
import org.apache.kafka.clients.producer.RecordMetadata
import zio.kafka.consumer.Consumer.AutoOffsetStrategy
import zio.{Task, UIO, ZIO, ZLayer, durationInt}

object KafkaContainerService {
  def sendBytes: ZIO[KafkaConfig, Throwable, RecordMetadata] =
    SimpleKafkaProducer.sendRecord("Some key", "Some value".getBytes)

  def sendJsonMessage: ZIO[KafkaConfig, Throwable, RecordMetadata] =
    SimpleKafkaProducer.sendRecordCirce("Some key", dummy("Some", "value"))

  def sendPlaintextMessage: ZIO[KafkaConfig, Throwable, RecordMetadata] =
    SimpleKafkaProducer.sendRecord("Some key", "Some value")

  def sendAvroMessage: ZIO[KafkaConfig, Throwable, RecordMetadata] =
    SimpleKafkaProducer.sendRecord("Some key", dummy("Some", "value"))

  val startContainer: Task[KafkaContainer] = ZIO.attemptBlocking {
    val container: KafkaContainer = new KafkaContainer()
    container.start()
    container
  }

  val stopContainer: KafkaContainer => UIO[Unit] = c => ZIO.succeedBlocking(c.stop())

  def kafkaConfigLayer(topicName:String): ZLayer[KafkaContainer, Nothing, KafkaConfig] = ZLayer.fromZIO {
    for {
      container <- ZIO.service[KafkaContainer]
    } yield KafkaConfig(
      topicName = SimpleKafkaProducer.topic,
      kafkaBrokers = List(container.bootstrapServers),
      consumerGroup = topicName,
      clientId = topicName,
      flushSeconds = 30.seconds,
      batchSize = 1,
      autoOffsetStrategy = AutoOffsetStrategy.Earliest,
      additionalProperties = Map.empty
    )
  }

  def topicLayer(topicName:String): ZLayer[Any, Throwable, KafkaContainer with KafkaConfig] = ZLayer
    .scoped {
      ZIO.acquireRelease(startContainer)(stopContainer)
    } >+> kafkaConfigLayer(topicName)
}
