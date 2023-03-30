package com.libertexgroup.ape.writers.kafka
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{ZIO, ZLayer}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream

protected[writers] class DefaultWriter[ET] extends KafkaWriter[KafkaConfig with ProducerSettings, ET, String, String] {
  override def apply(stream: ZStream[ET, Throwable, ProducerRecord[String, String]]):
  ZIO[KafkaConfig with ProducerSettings, Throwable, ZStream[ET, Throwable, ProducerRecord[String, String]]]
  =
    for {
      producerSettings <- ZIO.service[ProducerSettings]
      config <- ZIO.service[KafkaConfig]
      s = stream.tap(v => {
        ZIO.scoped {
          Producer.produce[Any, String, String](
            topic = config.topicName,
            key = v.key(),
            value = v.value(),
            keySerializer = Serde.string,
            valueSerializer = Serde.string
          ).provideSomeLayer(ZLayer.fromZIO(Producer.make(producerSettings)))
        }
      })

    } yield s
}
