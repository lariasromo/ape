package com.libertexgroup.ape.readers.kafka

import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.{Tag, ZIO}
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[kafka] class DefaultReader[Config <: KafkaConfig :Tag]
  extends KafkaReader[Config, Consumer, ConsumerRecord[String, Array[Byte]]] {

  def createStream(kafkaConfig: KafkaConfig): ZStream[Consumer, Throwable, ConsumerRecord[String, Array[Byte]]] =
    Consumer.subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
    .plainStream(Serde.string, Serde.byteArray)
    .tap { batch => batch.offset.commit }
    .map(record => record.record)
    .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
    .flatMap(r => ZStream.fromChunk(r))

  override protected[this] def read: ZIO[Config, Throwable, ZStream[Consumer, Throwable, ConsumerRecord[String, Array[Byte]]]] =
    for {
      kafkaConfig <- ZIO.service[Config]
    } yield createStream(kafkaConfig)
}
