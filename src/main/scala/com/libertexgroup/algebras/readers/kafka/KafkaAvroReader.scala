package com.libertexgroup.algebras.readers.kafka

import com.libertexgroup.configs.KafkaConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.clock.Clock
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Has, ZIO}

class KafkaAvroReader[T >:Null :SchemaFor :Decoder :Encoder] extends KafkaReader[Has[KafkaConfig], Consumer with Clock, ConsumerRecord[String, Option[T]]] {
  override def apply: ZIO[Has[KafkaConfig], Throwable, ZStream[Any with Consumer with Clock, Throwable, ConsumerRecord[String, Option[T]]]] =
    for {
        kafkaConfig <- ZIO.access[Has[KafkaConfig]](_.get)
        stream <- ZIO {
          Consumer.subscribeAnd( Subscription.topics( kafkaConfig.topicName ) )
          .plainStream(Serde.string, AvroUtils.getSerde[T])
          .filter(_.value.isDefined)
          .tap{batch => batch.offset.commit}
          .map(record => record.record)
          .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
          .flatMap(r => ZStream.fromChunk(r))
        }
    } yield stream
}
