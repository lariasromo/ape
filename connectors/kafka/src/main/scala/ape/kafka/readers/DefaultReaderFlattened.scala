package ape.kafka.readers

import ape.kafka.configs.KafkaConfig
import ape.utils.Utils.reLayer
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO}

protected[kafka] class DefaultReaderFlattened[Config <: KafkaConfig :Tag]
  extends KafkaReader[Config, Any, ConsumerRecord[String, Array[Byte]]] {
  override protected[this] def read: ZIO[Config, Throwable, ZStream[Any, Throwable, ConsumerRecord[String, Array[Byte]]]] =
    for {
      kafkaConfig <- ZIO.service[Config]
      l <- reLayer[Config]
    } yield Consumer.subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
      .plainStream(Serde.string, Serde.byteArray)
      .provideSomeLayer(l >>> KafkaConfig.liveConsumer)
      .tap { batch => batch.offset.commit }
      .map(record => record.record)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
      .flatMap(r => ZStream.fromChunk(r))
}
