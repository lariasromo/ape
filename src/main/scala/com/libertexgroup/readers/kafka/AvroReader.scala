package com.libertexgroup.readers.kafka

import com.libertexgroup.configs.KafkaConfig
import com.libertexgroup.utils.AvroUtils
import com.libertexgroup.utils.Utils.reLayer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO}

protected[kafka] class AvroReader[T >:Null :SchemaFor :Decoder :Encoder, Config <: KafkaConfig :Tag]
  extends KafkaReader[Config, Any, ConsumerRecord[String, Option[T]]] {

  override protected [this] def read:
    ZIO[Config, Throwable, ZStream[Any, Throwable, ConsumerRecord[String, Option[T]]]] =
    for {
        kafkaConfig <- ZIO.service[Config]
        l <- reLayer[Config]
    } yield Consumer.subscribeAnd( Subscription.topics( kafkaConfig.topicName ) )
      .plainStream(Serde.string, AvroUtils.getSerde[T])
      .provideSomeLayer(l >>> KafkaConfig.liveConsumer)
      .filter(_.value.isDefined)
      .tap{batch => batch.offset.commit}
      .map(record => record.record)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
      .flatMap(r => ZStream.fromChunk(r))
}
