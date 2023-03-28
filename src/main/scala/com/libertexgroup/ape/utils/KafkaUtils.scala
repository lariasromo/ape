package com.libertexgroup.ape.utils

import com.libertexgroup.configs.KafkaConfig
import zio.kafka.consumer.Consumer.OffsetRetrieval
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.{Scope, ZIO, ZLayer}

object KafkaUtils {
  def consumer: ZIO[Scope with KafkaConfig, Throwable, Consumer] = for {
    config <- ZIO.service[KafkaConfig]
    consumer <- Consumer.make(
      ConsumerSettings(config.kafkaBrokers)
        .withOffsetRetrieval(OffsetRetrieval.Auto(config.autoOffsetStrategy))
        .withGroupId(config.consumerGroup)
        .withClientId(config.consumerGroup)
    )
  } yield consumer

  val consumerLayer: ZLayer[KafkaConfig, Throwable, Consumer] = ZLayer.scoped(consumer)

  def producer: ZIO[Scope with KafkaConfig, Throwable, Producer] = for {
    config <- ZIO.service[KafkaConfig]
    producer <- Producer.make(
      ProducerSettings(config.kafkaBrokers).withClientId(config.consumerGroup)
    )
  } yield producer

  val producerLayer: ZLayer[KafkaConfig, Throwable, Producer] = ZLayer.scoped(producer)
}
