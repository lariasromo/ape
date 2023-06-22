package ape.datahub.configs

import ape.datahub.pipe.EmitterMechanism
import ape.datahub.pipe.EmitterMechanism.EmitterMechanism
import com.linkedin.common.FabricType
import datahub.client.kafka.KafkaEmitterConfig
import datahub.client.rest.RestEmitterConfig
import zio.System.envOrElse
import zio.{ZIO, ZLayer}

import java.util.function.Consumer

case class DatahubConfig(
                          fabricType: FabricType,
                          mechanism: EmitterMechanism.Value,
                          restEmitterUrl:String = "http://localhost:8080",
                          restEmitterToken:String = "",
                          schemaRegistryUrl:String = "http://localhost:8080",
                          kafkaBootstrapServers:String = "localhost:9092",
                          tags: Seq[String] = Seq.empty
                        ) {

  def getRestEmitterConfig: Consumer[RestEmitterConfig.RestEmitterConfigBuilder] = {
    b => b.server(restEmitterUrl).token(restEmitterToken)
  }

  def getKafkaEmitterConfig: KafkaEmitterConfig = {
    KafkaEmitterConfig
      .builder
      .schemaRegistryUrl(schemaRegistryUrl)
      .bootstrap(kafkaBootstrapServers)
      .build
  }
}

object DatahubConfig {
  def make(prefix:Option[String]=None): ZIO[Any, SecurityException, DatahubConfig] = for {
    fabricType <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "DATAHUB_FABRIC_TYPE", "DEV")
    emitterMechanism <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "DATAHUB_EMITTER_MECHANISM", "REST")
    restEmitterUrl <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "DATAHUB_REST_EMITTER_URL", "")
    restEmitterToken <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "DATAHUB_REST_EMITTER_TOKEN", "")
    schemaRegistryUrl <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "DATAHUB_SCHEMA_REGISTRY_URL", "")
    kafkaBootstrapServers <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "DATAHUB_KAFKA_BOOTSTRAP_SERVERS", "")
    tags <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "DATAHUB_TAGS", "")
  } yield DatahubConfig(
    fabricType = FabricType.valueOf(fabricType),
    mechanism = EmitterMechanism.withName(emitterMechanism.toUpperCase),
    restEmitterUrl = restEmitterUrl,
    restEmitterToken = restEmitterToken,
    schemaRegistryUrl = schemaRegistryUrl,
    kafkaBootstrapServers = kafkaBootstrapServers,
    tags = tags.split(",")
  )

  def live(prefix:Option[String]=None): ZLayer[Any, SecurityException, DatahubConfig] = ZLayer.fromZIO(make(prefix))
}