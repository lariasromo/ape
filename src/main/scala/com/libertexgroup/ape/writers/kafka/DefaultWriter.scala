package com.libertexgroup.ape.writers.kafka
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{ZIO, ZLayer}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream

protected[writers] class DefaultWriter[E] extends KafkaWriter[E, E with Producer with KafkaConfig, String, String] {
  override def apply(stream: ZStream[E, Throwable, ProducerRecord[String, String]]):
    ZIO[E with Producer with KafkaConfig, Throwable, Unit] =
    for {
      config <- ZIO.service[KafkaConfig]
      _ <- stream.tap(v => {
          Producer.produce[Any, String, String](
            topic = config.topicName,
            key = v.key(),
            value = v.value(),
            keySerializer = Serde.string,
            valueSerializer = Serde.string
          )
        }).runDrain

    } yield ()

}