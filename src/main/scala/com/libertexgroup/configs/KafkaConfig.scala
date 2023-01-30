package com.libertexgroup.configs

import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.{Duration, ZIO, ZLayer, durationInt}
import zio.System.{env, envOrElse}

import scala.util.Try

case class KafkaConfig(
                        topicName: String,
                        kafkaBrokers: List[String],
                        consumerGroup: String,
                        flushSeconds: Duration,
                        batchSize: Int
                      )


object KafkaConfig extends ReaderConfig {
  def make: ZIO[Any, SecurityException, KafkaConfig] = for {
    kafkaBrokers <- envOrElse("KAFKA_BROKERS", "")
    consumerGroup <- envOrElse("KAFKA_CONSUMER_GROUP", "")
    topicName <- envOrElse("KAFKA_TOPIC", "")
    flushSeconds <- envOrElse("KAFKA_FLUSH_SECONDS", "300")
    batchSize <- envOrElse("KAFKA_BATCH_SIZE", "10000")
  } yield KafkaConfig(
    topicName,
    kafkaBrokers.split(",").toList,
    consumerGroup,
    Try(flushSeconds.toInt).toOption.getOrElse(300).seconds,
    Try(batchSize.toInt).toOption.getOrElse(1000)
  )

  def live: ZLayer[Any, SecurityException, KafkaConfig] = ZLayer.fromZIO(make)

  val kafkaConsumer: ZIO[KafkaConfig, Nothing, ZLayer[Any, Throwable, Consumer]] = for {
    config <- ZIO.service[KafkaConfig]
    _ <- ZIO.when(config.kafkaBrokers.isEmpty || config.kafkaBrokers.head.isEmpty) {
      ZIO.fail(throw new Exception("Kafka Brokers are empty"))
    }
  } yield ZLayer.scoped(
    Consumer.make(
      ConsumerSettings(config.kafkaBrokers).withGroupId(config.consumerGroup)
    )
  )
}
