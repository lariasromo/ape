package com.libertexgroup.ape.writers.kafka

import com.libertexgroup.configs.KafkaConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.{ZSink, ZStream}
import zio.{Scope, ZIO, ZLayer}

protected[writers] class AvroWriter[E, T:SchemaFor :Encoder] extends KafkaWriter[E, E with Producer with KafkaConfig with Scope, String, T] {
  override def apply(stream: ZStream[E, Throwable, ProducerRecord[String, T]]):
    ZIO[Producer with E with KafkaConfig with Scope, Throwable, Unit] =
    for {
      config <- ZIO.service[KafkaConfig]
      _ <- stream.tap(v => {
        import com.libertexgroup.ape.utils.AvroUtils.implicits._
          Producer.produce[Any, String, Array[Byte]](
            topic = config.topicName,
            key = v.key(),
            value = v.value().encode.orNull,
            keySerializer = Serde.string,
            valueSerializer = Serde.byteArray
          )
        }).runScoped(ZSink.drain)

    } yield ()

}
