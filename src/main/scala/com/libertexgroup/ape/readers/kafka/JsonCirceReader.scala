package com.libertexgroup.ape.readers.kafka

import com.libertexgroup.configs.KafkaConfig
import io.circe.{Decoder, jawn}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.ZIO
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[readers] class JsonCirceReader[T: Decoder :ClassTag]
  extends com.libertexgroup.ape.readers.kafka.KafkaReader[KafkaConfig, Consumer, ConsumerRecord[String, T]] {

  override def apply: ZIO[KafkaConfig, Throwable, ZStream[Any with Consumer, Throwable, ConsumerRecord[String, T]]] =
    for {
        kafkaConfig <- ZIO.service[KafkaConfig]
    } yield Consumer.subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
      .plainStream(Serde.string, Serde.string)
      .tap { batch => batch.offset.commit }
      .map(record => record.record)
      .map(r => {
        jawn.decode[T](r.value()).toOption.map(value => {
          new ConsumerRecord(r.topic(), r.partition(), r.offset(), r.timestamp(), r.timestampType(),
            r.serializedKeySize(), r.serializedValueSize(), r.key(), value, r.headers(), r.leaderEpoch())
        })
      }).filter(_.isDefined).map(_.get)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
      .flatMap(r => ZStream.fromChunk(r))
}
