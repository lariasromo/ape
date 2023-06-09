package ape.kafka.pipes

import ape.kafka.configs.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.{Tag, ZIO, ZLayer}
import zio.stream.ZStream


protected[kafka]  class DefaultPipeWithCompleteRecord [ET, Config <: KafkaConfig :Tag] extends KafkaPipe[Config, ET, String, String] {
  override protected[this] def pipe(i: ZStream[ET, Throwable, ProducerRecord[String, String]]):
  ZIO[Config, Throwable, ZStream[ET, Throwable, ProducerRecord[String, String]]]
    = for {
    config <- ZIO.service[Config]
    s = i.tap(v => {
      val producerRecord =  new ProducerRecord[String,String](
        config.topicName,  0,  v.key(), v.value(), v.headers()  )
      ZIO.scoped {
        Producer.produce[Any, String, String](
          record = producerRecord,
          keySerializer = Serde.string,
          valueSerializer = Serde.string
        ).provideSomeLayer(ZLayer.fromZIO(Producer.make(config.producerSettings)))
      }
    })
  } yield s


}
