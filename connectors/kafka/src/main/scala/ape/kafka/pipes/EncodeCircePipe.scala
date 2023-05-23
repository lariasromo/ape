package ape.kafka.pipes

import ape.kafka.configs.KafkaConfig
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO, ZLayer}

import scala.reflect.ClassTag

protected[kafka] class EncodeCircePipe[ET, Config <: KafkaConfig :Tag, T :ClassTag :Encoder]
  extends KafkaPipe[Config, ET, String, T] {

  override protected[this] def pipe(i: ZStream[ET, Throwable, ProducerRecord[String, T]]):
    ZIO[Config, Throwable, ZStream[ET, Throwable, ProducerRecord[String, T]]] = for {
    config <- ZIO.service[Config]
    s = i.tap(v => {
      ZIO.scoped {
        Producer.produce[Any, String, String](
          topic = config.topicName,
          key = v.key(),
          value = v.value().asJson.noSpaces,
          keySerializer = Serde.string,
          valueSerializer = Serde.string
        ).provideSomeLayer(ZLayer.fromZIO(Producer.make(config.producerSettings)))
      }
    })
  } yield s
}
