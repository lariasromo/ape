package services

import com.dimafeng.testcontainers.KafkaContainer
import com.libertexgroup.configs.KafkaConfig
import zio._
import zio.blocking.{Blocking, effectBlocking}
import zio.duration.durationInt

object KafkaContainerService {
  type KafkaTestConfig = Has[ContainersAndConfigs]

  case class ContainersAndConfigs(
                                   container: KafkaContainer,
                                   kafkaConfig: KafkaConfig
                                 )

  def kafka: ZLayer[Blocking, Nothing, KafkaTestConfig] = ZManaged.make {
    effectBlocking {
      val container = KafkaContainer()
      container.start()

      val kafkaConfig = KafkaConfig(
        topicName = "testTopic",
        kafkaBrokers = List(container.bootstrapServers),
        consumerGroup = "testGroup",
        flushSeconds = 60.seconds,
        batchSize = 1000
      )

      ContainersAndConfigs(container, kafkaConfig)
    }.orDie
  }(containersAndConfigs => {
    effectBlocking(containersAndConfigs.container.stop()).orDie
  }).toLayer


  val kafkaConfigLayer: ZLayer[Blocking, Nothing, Has[KafkaConfig]] = ZLayer.fromEffect {
    (
      for {
        config <- ZIO.access[Has[ContainersAndConfigs]](_.get)
      } yield config.kafkaConfig
    ).provideLayer(kafka)
  }
}