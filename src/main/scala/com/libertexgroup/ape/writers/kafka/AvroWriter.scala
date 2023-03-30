package com.libertexgroup.ape.writers.kafka

import com.libertexgroup.configs.KafkaConfig
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Scope, ZIO, ZLayer}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream

protected[writers] class AvroWriter[ET, T:SchemaFor :Encoder] extends
  KafkaWriter[KafkaConfig with ProducerSettings, ET, String, T] {
  override def apply(stream: ZStream[ET, Throwable, ProducerRecord[String, T]]):
  ZIO[KafkaConfig with ProducerSettings, Nothing, ZStream[ET, Throwable, ProducerRecord[String, T]]]
  =
    for {
      producerSettings <- ZIO.service[ProducerSettings]
      config <- ZIO.service[KafkaConfig]
    } yield {
        stream.tap(v => {
          import com.libertexgroup.ape.utils.AvroUtils.implicits._
          ZIO.scoped {
            Producer.produce[Any, String, Array[Byte]](
              topic = config.topicName,
              key = v.key(),
              value = v.value().encode.orNull,
              keySerializer = Serde.string,
              valueSerializer = Serde.byteArray
            ).provideSomeLayer(ZLayer.fromZIO(Producer.make(producerSettings)))
          }
        })
      }
}
