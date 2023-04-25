package com.libertexgroup.ape.readers.kafka

import com.libertexgroup.ape.utils.AvroUtils
import com.libertexgroup.configs.KafkaConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO}

protected[kafka] class AvroReader[T >:Null :SchemaFor :Decoder :Encoder, Config <: KafkaConfig :Tag]
  extends KafkaReader[Config, Config, ConsumerRecord[String, Option[T]]] {

  override protected [this] def read:
    ZIO[Config, Throwable, ZStream[Config, Throwable, ConsumerRecord[String, Option[T]]]] =
    for {
        kafkaConfig <- ZIO.service[Config]
    } yield Consumer.subscribeAnd( Subscription.topics( kafkaConfig.topicName ) )
      .plainStream(Serde.string, AvroUtils.getSerde[T])
      .provideSomeLayer(KafkaConfig.liveConsumer)
      .filter(_.value.isDefined)
      .tap{batch => batch.offset.commit}
      .map(record => record.record)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
      .flatMap(r => ZStream.fromChunk(r))
}
