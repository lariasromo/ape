package com.libertexgroup.algebras.readers.kafka

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.clock.Clock
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Has, ZIO}

object KafkaDefaultReader extends Reader {
  override type Env = Has[KafkaConfig]
  override type Env2 = Any with Consumer with Clock
  override type StreamType = ConsumerRecord[String, Array[Byte]]

  override def apply: ZIO[Has[KafkaConfig], Throwable,
    ZStream[Any with Consumer with Clock, Throwable, ConsumerRecord[String, Array[Byte]]]] =
    for {
        kafkaConfig <- ZIO.access[Has[KafkaConfig]](_.get)
        stream <- ZIO {
          Consumer.subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
          .plainStream(Serde.string, Serde.byteArray)
          .tap { batch => batch.offset.commit }
          .map(record => record.record)
          .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
          .flatMap(r => ZStream.fromChunk(r))
        }
    } yield stream
}
