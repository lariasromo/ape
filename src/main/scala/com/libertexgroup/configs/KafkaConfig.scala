package com.libertexgroup.configs

import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.{Duration, durationInt}
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.{Has, ZIO, ZLayer, system}

import scala.util.Try

case class KafkaConfig(
                        topicName: String,
                        kafkaBrokers: List[String],
                        consumerGroup: String,
                        flushSeconds: Duration,
                        batchSize: Int
                      )


object KafkaConfig extends ReaderConfig {
  def make: ZIO[system.System, SecurityException, KafkaConfig] = for {
    kafkaBrokers <- system.envOrElse("KAFKA_BROKERS", "")
    consumerGroup <- system.envOrElse("KAFKA_CONSUMER_GROUP", "")
    topicName <- system.envOrElse("KAFKA_TOPIC", "")
    flushSeconds <- system.envOrElse("KAFKA_FLUSH_SECONDS", "300")
    batchSize <- system.envOrElse("KAFKA_BATCH_SIZE", "10000")
  } yield KafkaConfig(
    topicName,
    kafkaBrokers.split(",").toList,
    consumerGroup,
    Try(flushSeconds.toInt).toOption.getOrElse(300).seconds,
    Try(batchSize.toInt).toOption.getOrElse(1000)
  )

  def live: ZLayer[system.System, SecurityException, Has[KafkaConfig]] = ZLayer.fromEffect(make)

  val kafkaConsumer: ZIO[Has[KafkaConfig], Nothing, ZLayer[Clock with Blocking, Throwable, Has[Consumer.Service]]] = for {
    config <- ZIO.access[Has[KafkaConfig]](_.get)
    _ <- ZIO.when(config.kafkaBrokers.isEmpty || config.kafkaBrokers.head.isEmpty) {
      ZIO.fail(throw new Exception("Kafka Brokers are empty"))
    }
  } yield ZLayer.fromManaged(
    Consumer.make(
      ConsumerSettings(config.kafkaBrokers).withGroupId(config.consumerGroup)
    )
  )
}
