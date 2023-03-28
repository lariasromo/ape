package com.libertexgroup.ape.writers.kafka

import com.libertexgroup.configs.KafkaConfig
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.ZIO
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.stream.ZStream

protected[writers] class AvroWriter[ET, T:SchemaFor :Encoder] extends
  KafkaWriter[KafkaConfig, Producer with ET, String, T] {
  override def apply(stream: ZStream[Producer with ET, Throwable, ProducerRecord[String, T]]):
  ZIO[KafkaConfig, Throwable, ZStream[Producer with ET, Throwable, ProducerRecord[String, T]]] =
    for {
      config <- ZIO.service[KafkaConfig]
    } yield stream.tap(v => {
      import com.libertexgroup.ape.utils.AvroUtils.implicits._
      Producer.produce[Any, String, Array[Byte]](
        topic = config.topicName,
        key = v.key(),
        value = v.value().encode.orNull,
        keySerializer = Serde.string,
        valueSerializer = Serde.byteArray
      )
    })
}
