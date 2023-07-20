package ape.kafka.configs

import zio.System.{envOrElse, envs}
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.{Duration, Scope, ZIO, ZLayer, durationInt}

import scala.util.Try
import zio.config.ZConfig
import zio.config.magnolia.descriptor

case class KafkaConfig(
                        topicName: String,
                        kafkaBrokers: List[String],
                        consumerGroup: String,
                        clientId: String,
                        flushSeconds: Duration = 1.seconds,
                        batchSize: Int = 1,
                        autoOffsetStrategy: AutoOffsetStrategy=AutoOffsetStrategy.Latest,
                        pollInterval: Duration = 50.milliseconds,
                        pollTimeout: Duration = 50.milliseconds,
                        closeTimeout: Duration = 50.milliseconds,
                        restartStreamOnRebalancing: Boolean = true,
                        additionalProperties: Map[String, String]=Map.empty
  ){
  val producerSettings: ProducerSettings = ProducerSettings(kafkaBrokers)
        .withClientId(consumerGroup)
        .withProperties(additionalProperties)

  val consumerSettings: ConsumerSettings = ConsumerSettings(kafkaBrokers)
        .withOffsetRetrieval(OffsetRetrieval.Auto(autoOffsetStrategy))
        .withGroupId(consumerGroup)
        .withClientId(clientId)
        .withRestartStreamOnRebalancing(false)
        .withPollInterval(pollInterval)
        .withPollTimeout(pollTimeout)
        .withProperties(additionalProperties)
}


object KafkaConfig {

  val configDescriptor = descriptor[KafkaConfig]
  val liveMagnolia: ZLayer[Any, Throwable, KafkaConfig] = ZConfig.fromSystemEnv(configDescriptor)

  def make(prefix:Option[String]=None): ZIO[Any, SecurityException, KafkaConfig] = {
    val p = prefix.map(s => s + "_").getOrElse("")
    for {
      clientId <- envOrElse(p + "KAFKA_CLIENT_ID", "")
      offsetStrategy <- envOrElse(p + "KAFKA_OFFSET_STRATEGY", "")
      kafkaBrokers <- envOrElse(p + "KAFKA_BROKERS", "")
      consumerGroup <- envOrElse(p + "KAFKA_CONSUMER_GROUP", "")
      topicName <- envOrElse(p + "KAFKA_TOPIC", "")
      flushSeconds <- envOrElse(p + "KAFKA_FLUSH_SECONDS", "300")
      batchSize <- envOrElse(p + "KAFKA_BATCH_SIZE", "10000")
      pollInterval <- envOrElse(p + "KAFKA_POLL_INTERVAL", "50")
      pollTimeout <- envOrElse(p + "KAFKA_POLL_TIMEOUT", "50")
      closeTimeout <- envOrElse(p + "KAFKA_CLOSE_TIMEOUT", "60000")
      restartStreamOnRebalancing <- envOrElse(p + "KAFKA_RESTART_STREAM_ON_REBALANCING", "true")
      envs <- envs
      additionalProperties = envs
        .filter(e => e._1.startsWith(p + "KAFKA_PROP_"))
        .map(e => (
            e._1.replace(p + "KAFKA_PROP_", "").replace("_", ".").toLowerCase(),
            e._2
          ))
    } yield KafkaConfig(
      topicName = topicName,
      kafkaBrokers = kafkaBrokers.split(",").toList,
      consumerGroup = consumerGroup,
      flushSeconds = Try(flushSeconds.toInt).toOption.getOrElse(300).seconds,
      batchSize = Try(batchSize.toInt).toOption.getOrElse(1000),
      autoOffsetStrategy = {
        if (offsetStrategy.equalsIgnoreCase("latest"))
          AutoOffsetStrategy.Latest
        else AutoOffsetStrategy.Earliest
      },
      additionalProperties = additionalProperties,
      clientId = clientId,
      pollInterval = Try(pollInterval.toInt).toOption.getOrElse(50).milliseconds,
      pollTimeout = Try(pollTimeout.toInt).toOption.getOrElse(50).milliseconds,
      closeTimeout = Try(closeTimeout.toInt).toOption.getOrElse(60000).milliseconds,
      restartStreamOnRebalancing = restartStreamOnRebalancing.toLowerCase.equals("true")
    )
  }

  def live(prefix:Option[String]=None): ZLayer[Any, SecurityException, KafkaConfig] = ZLayer.fromZIO(make(prefix))

  def makeConsumer: ZIO[Scope with KafkaConfig, Throwable, Consumer] = for {
    config <- ZIO.service[KafkaConfig]
    consumer <- Consumer.make(config.consumerSettings)
  } yield consumer

  val liveConsumer: ZLayer[KafkaConfig, Throwable, Consumer] = ZLayer.scoped(makeConsumer)

  def makeProducer: ZIO[Scope with KafkaConfig, Throwable, Producer] = for {
    config <- ZIO.service[KafkaConfig]
    producer <- Producer.make(config.producerSettings)
  } yield producer

  val liveProducer: ZLayer[KafkaConfig, Throwable, Producer] = ZLayer.scoped(makeProducer)
}
