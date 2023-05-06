package com.libertexgroup.readers.kafka

import com.libertexgroup.configs.KafkaConfig
import com.libertexgroup.utils.Utils.reLayer
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO}

protected[kafka] class StringReader[Config <: KafkaConfig :Tag]
  extends KafkaReader[Config, Any, ConsumerRecord[String, String]] {

  override protected[this] def read: ZIO[Config, Throwable, ZStream[Any, Throwable, ConsumerRecord[String, String]]] =
    for {
      kafkaConfig <- ZIO.service[Config]
      l <- reLayer[Config]
    } yield Consumer.subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
      .plainStream(Serde.string, Serde.string)
      .provideSomeLayer(l >>> KafkaConfig.liveConsumer)
      .tap { batch => batch.offset.commit }
      .map(record => record.record)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
      .flatMap(r => ZStream.fromChunk(r))
}
