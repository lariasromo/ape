package com.libertexgroup.algebras.readers.kafka

import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.ZIO
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

class KafkaDefaultReader extends KafkaReader[KafkaConfig, Consumer, ConsumerRecord[String, Array[Byte]]] {
  override def apply:
  ZIO[KafkaConfig, Throwable, ZStream[Any with Consumer, Throwable, ConsumerRecord[String, Array[Byte]]]]
  =
    for {
        kafkaConfig <- ZIO.service[KafkaConfig]
    } yield Consumer.subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
      .plainStream(Serde.string, Serde.byteArray)
      .tap { batch => batch.offset.commit }
      .map(record => record.record)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
      .flatMap(r => ZStream.fromChunk(r))
}
