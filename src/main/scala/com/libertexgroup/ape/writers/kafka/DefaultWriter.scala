package com.libertexgroup.ape.writers.kafka
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Tag, ZIO, ZLayer}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream

protected[kafka] class DefaultWriter[ET, Config <: KafkaConfig :Tag]
  extends KafkaWriter[Config, ET, String, String] {
  override def apply(stream: ZStream[ET, Throwable, ProducerRecord[String, String]]):
  ZIO[Config, Throwable, ZStream[ET, Throwable, ProducerRecord[String, String]]] =
    for {
      config <- ZIO.service[Config]
      s = stream.tap(v => {
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
