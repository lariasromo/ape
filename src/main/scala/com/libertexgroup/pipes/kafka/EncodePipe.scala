package com.libertexgroup.pipes.kafka

import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO, ZLayer}

import scala.reflect.ClassTag

protected[kafka] class EncodePipe[ET, Config <: KafkaConfig :Tag, T :ClassTag](implicit val enc: T => String)
  extends KafkaPipe[Config, ET, String, T] {

  override protected[this] def pipe(i: ZStream[ET, Throwable, ProducerRecord[String, T]]):
    ZIO[Config, Throwable, ZStream[ET, Throwable, ProducerRecord[String, T]]] = for {
    config <- ZIO.service[Config]
    s = i.tap(v => {
      ZIO.scoped {
        Producer.produce[Any, String, String](
          topic = config.topicName,
          key = v.key(),
          value = enc(v.value()),
          keySerializer = Serde.string,
          valueSerializer = Serde.string
        ).provideSomeLayer(ZLayer.fromZIO(Producer.make(config.producerSettings)))
      }
    })
  } yield s
}
