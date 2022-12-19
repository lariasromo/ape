package com.libertexgroup.configs

import zio.duration.{Duration, durationInt}
import zio.{Has, ZIO, ZLayer, system}

import scala.util.Try

case class KafkaConfig(
                        topicName: String,
                        kafkaBrokers: List[String],
                        consumerGroup: String,
                        flushSeconds: Duration,
                        batchSize: Int
                      )


object KafkaConfig extends ReaderConfig {
  def make: ZIO[system.System, SecurityException, KafkaConfig] = for {
    kafkaBrokers <- system.envOrElse("KAFKA_BROKERS", throw new Exception("Variable KAFKA_BROKERS should be set"))
    consumerGroup <- system.envOrElse("CONSUMER_GROUP", throw new Exception("Variable CONSUMER_GROUP should be set"))
    topicName <- system.envOrElse("TOPIC", throw new Exception("Variable TOPIC should be set"))
    flushSeconds <- system.env("KAFKA_FLUSH_SECONDS")
    batchSize <- system.env("KAFKA_BATCH_SIZE")
  } yield KafkaConfig(
    topicName,
    kafkaBrokers.split(",").toList,
    consumerGroup,
    Try(flushSeconds.map(_.toInt)).toOption.flatten.getOrElse(300).seconds,
    Try(batchSize.map(_.toInt)).toOption.flatten.getOrElse(1000)
  )

  def live: ZLayer[system.System, SecurityException, Has[KafkaConfig]] = ZLayer.fromEffect(make)
}
