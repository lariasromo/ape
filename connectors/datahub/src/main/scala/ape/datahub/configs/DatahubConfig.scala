package ape.datahub.configs

import com.linkedin.common.FabricType
import datahub.client.kafka.KafkaEmitterConfig
import datahub.client.rest.RestEmitterConfig

import java.util.function.Consumer

case class DatahubConfig(fabricType: FabricType) {
  def getRestEmitterConfig: Consumer[RestEmitterConfig.RestEmitterConfigBuilder] = {
    b => b.server("http://localhost:8080")
  }

  def getKafkaEmitterConfig: KafkaEmitterConfig = {
    KafkaEmitterConfig.builder.build
  }
}