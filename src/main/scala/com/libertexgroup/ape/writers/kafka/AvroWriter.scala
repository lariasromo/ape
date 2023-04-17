package com.libertexgroup.ape.writers.kafka

import com.libertexgroup.configs.KafkaConfig
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO, ZLayer}

protected[kafka] class AvroWriter[ET, T:SchemaFor :Encoder, Config <: KafkaConfig :Tag] extends
  KafkaWriter[Config, ET, String, T] {

  override protected[this] def pipe(i: ZStream[ET, Throwable, ProducerRecord[String, T]]):
    ZIO[Config, Throwable, ZStream[ET, Throwable, ProducerRecord[String, T]]] = for {
    config <- ZIO.service[Config]
  } yield {
    i.tap(v => {
      import com.libertexgroup.ape.utils.AvroUtils.implicits._
      ZIO.scoped {
        Producer.produce[Any, String, Array[Byte]](
          topic = config.topicName,
          key = v.key(),
          value = v.value().encode.orNull,
          keySerializer = Serde.string,
          valueSerializer = Serde.byteArray
        ).provideSomeLayer(ZLayer.fromZIO(Producer.make(config.producerSettings)))
      }
    })
  }
}
