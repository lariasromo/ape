package com.libertexgroup.configs

import zio.System.envOrElse
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.{Duration, Scope, ZIO, ZLayer, durationInt}

import scala.util.Try

case class KafkaConfig(
                        topicName: String,
                        kafkaBrokers: List[String],
                        consumerGroup: String,
                        flushSeconds: Duration,
                        batchSize: Int,
                        autoOffsetStrategy: AutoOffsetStrategy
  ){
  val producerSettings: ProducerSettings = ProducerSettings(kafkaBrokers)
    .withClientId(consumerGroup)

  val consumerSettings: ConsumerSettings = ConsumerSettings(kafkaBrokers)
    .withOffsetRetrieval(OffsetRetrieval.Auto(autoOffsetStrategy))
    .withGroupId(consumerGroup)
    .withClientId(consumerGroup)
}


object KafkaConfig extends ReaderConfig {
  def make: ZIO[Any, SecurityException, KafkaConfig] = for {
    offsetStrategy <- envOrElse("KAFKA_OFFSET_STRATEGY", "")
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
    Try(batchSize.toInt).toOption.getOrElse(1000),
    if(offsetStrategy.equalsIgnoreCase("latest")) AutoOffsetStrategy.Latest else AutoOffsetStrategy.Earliest
  )

  def live: ZLayer[Any, SecurityException, KafkaConfig] = ZLayer.fromZIO(make)

  def makeConsumer: ZIO[Scope with KafkaConfig, Throwable, Consumer] = for {
    config <- ZIO.service[KafkaConfig]
    consumer <- Consumer.make(
      ConsumerSettings(config.kafkaBrokers)
        .withOffsetRetrieval(OffsetRetrieval.Auto(config.autoOffsetStrategy))
        .withGroupId(config.consumerGroup)
        .withClientId(config.consumerGroup)
    )
  } yield consumer

  val liveConsumer: ZLayer[KafkaConfig, Throwable, Consumer] = ZLayer.scoped(makeConsumer)

  def makeProducer: ZIO[Scope with KafkaConfig, Throwable, Producer] = for {
    config <- ZIO.service[KafkaConfig]
    producer <- Producer.make(
      ProducerSettings(config.kafkaBrokers).withClientId(config.consumerGroup)
    )
  } yield producer

  val liveProducer: ZLayer[KafkaConfig, Throwable, Producer] = ZLayer.scoped(makeProducer)
}
