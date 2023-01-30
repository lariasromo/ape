package com.libertexgroup.algebras.readers.kafka

import com.libertexgroup.configs.KafkaConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.ZIO
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

class KafkaAvroReader[T >:Null :SchemaFor :Decoder :Encoder] extends KafkaReader[KafkaConfig, Consumer, ConsumerRecord[String, Option[T]]] {
  override def apply: ZIO[KafkaConfig, Throwable, ZStream[Any with Consumer, Throwable, ConsumerRecord[String, Option[T]]]] =
    for {
        kafkaConfig <- ZIO.service[KafkaConfig]
    } yield Consumer.subscribeAnd( Subscription.topics( kafkaConfig.topicName ) )
      .plainStream(Serde.string, AvroUtils.getSerde[T])
      .filter(_.value.isDefined)
      .tap{batch => batch.offset.commit}
      .map(record => record.record)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
      .flatMap(r => ZStream.fromChunk(r))
}
