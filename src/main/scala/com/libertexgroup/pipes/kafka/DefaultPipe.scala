package com.libertexgroup.pipes.kafka

import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO, ZLayer}

protected[kafka] class DefaultPipe[ET, Config <: KafkaConfig :Tag] extends KafkaPipe[Config, ET, String, String] {
  override protected[this] def pipe(i: ZStream[ET, Throwable, ProducerRecord[String, String]]):
    ZIO[Config, Throwable, ZStream[ET, Throwable, ProducerRecord[String, String]]] = for {
    config <- ZIO.service[Config]
    s = i.tap(v => {
      ZIO.scoped {
        Producer.produce[Any, String, String](
          topic = config.topicName,
          key = v.key(),
          value = v.value(),
          keySerializer = Serde.string,
          valueSerializer = Serde.string
        ).provideSomeLayer(ZLayer.fromZIO(Producer.make(config.producerSettings)))
      }
    })
  } yield s
}
