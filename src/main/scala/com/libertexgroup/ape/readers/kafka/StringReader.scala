package com.libertexgroup.ape.readers.kafka

import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.{Tag, ZIO}
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

protected[kafka] class StringReader[Config <: KafkaConfig :Tag]
  extends KafkaReader[Config, Consumer, ConsumerRecord[String, String]] {

  override protected[this] def read: ZIO[Config, Throwable, ZStream[Consumer, Throwable, ConsumerRecord[String, String]]] =
    for {
      kafkaConfig <- ZIO.service[Config]
    } yield Consumer.subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
      .plainStream(Serde.string, Serde.string)
      .tap { batch => batch.offset.commit }
      .map(record => record.record)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
      .flatMap(r => ZStream.fromChunk(r))
}
