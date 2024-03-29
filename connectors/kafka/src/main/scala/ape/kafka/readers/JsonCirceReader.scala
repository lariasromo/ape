package ape.kafka.readers

import ape.kafka.configs.KafkaConfig
import ape.utils.Utils.reLayer
import io.circe.{Decoder, jawn}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO}

import scala.reflect.ClassTag

protected[kafka] class JsonCirceReader[T: Decoder :ClassTag, Config <: KafkaConfig :Tag]
  extends KafkaReader[Config, Any, Chunk[ConsumerRecord[String, T]]] {

  override protected[this] def read: ZIO[Config, Throwable, ZStream[Any, Throwable, Chunk[ConsumerRecord[String, T]]]] =
    for {
      kafkaConfig <- ZIO.service[Config]
      l <- reLayer[Config]
    } yield Consumer.subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
      .plainStream(Serde.string, Serde.string)
      .provideSomeLayer(l >>> KafkaConfig.liveConsumer)
      .tap { batch => batch.offset.commit }
      .map(record => record.record)
      .map(r => {
        jawn.decode[T](r.value()).toOption.map(value => {
          new ConsumerRecord(r.topic(), r.partition(), r.offset(), r.timestamp(), r.timestampType(),
            r.serializedKeySize(), r.serializedValueSize(), r.key(), value, r.headers(), r.leaderEpoch())
        })
      }).filter(_.isDefined).map(_.get)
      .groupedWithin(kafkaConfig.batchSize, kafkaConfig.flushSeconds)
}
