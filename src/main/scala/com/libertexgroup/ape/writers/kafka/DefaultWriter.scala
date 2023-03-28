package com.libertexgroup.ape.writers.kafka
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.ZIO
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.stream.ZStream

protected[writers] class DefaultWriter[ET] extends KafkaWriter[KafkaConfig, Producer with ET, String, String] {
  override def apply(stream: ZStream[Producer with ET, Throwable, ProducerRecord[String, String]]):
  ZIO[KafkaConfig, Throwable, ZStream[Producer with ET, Throwable, ProducerRecord[String, String]]] =
    for {
      config <- ZIO.service[KafkaConfig]
      s = stream.tap(v => {
        Producer.produce[Any, String, String](
          topic = config.topicName,
          key = v.key(),
          value = v.value(),
          keySerializer = Serde.string,
          valueSerializer = Serde.string
        )
      })

    } yield s
}
