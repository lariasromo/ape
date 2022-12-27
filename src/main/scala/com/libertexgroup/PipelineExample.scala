package com.libertexgroup

import com.libertexgroup.algebras.pipelines.Pipeline
import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.algebras.readers.kafka.KafkaDefaultReader
import com.libertexgroup.algebras.transformers.Transformer
import com.libertexgroup.algebras.transformers.clickhouse.DefaultClickhouseTransformer
import com.libertexgroup.algebras.writers.{Writer, clickhouse}
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig}
import com.libertexgroup.models.ClickhouseModel
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.kafka.consumer.Consumer
import zio.{ExitCode, Has, URIO, ZIO, ZLayer, system}

object PipelineExample extends zio.App {
  val reader: Reader[Has[KafkaConfig], Consumer with Clock, ConsumerRecord[String, Array[Byte]]] = new KafkaDefaultReader()
  val transformer: Transformer[Consumer with Clock, ConsumerRecord[String, Array[Byte]], ClickhouseModel] =
    new DefaultClickhouseTransformer[Consumer with Clock]()
  val writer: Writer[Consumer with Clock, Consumer with Clock with Has[ClickhouseConfig], ClickhouseModel] =
    new clickhouse.DefaultWriter[Consumer with Clock]()

  type pipeType = Console with Has[KafkaConfig] with Clock with Has[ClickhouseConfig]
    with Has[Reader[Has[KafkaConfig], Consumer with Clock, ConsumerRecord[String, Array[Byte]]]]
    with Has[Transformer[Consumer with Clock, ConsumerRecord[String, Array[Byte]], ClickhouseModel]]
    with Has[Writer[Consumer with Clock, Consumer with Clock with Has[ClickhouseConfig], ClickhouseModel]]


  def getLayer: ZLayer[Any with Blocking with system.System, Throwable, pipeType] = Clock.live ++ Console.live ++
    ZLayer.succeed(reader) ++
    ZLayer.succeed(transformer) ++
    ZLayer.succeed(writer) ++
    KafkaConfig.live ++
    ClickhouseConfig.live

  val layerEffect: ZIO[Any with Blocking with system.System, Any, ZLayer[Clock with Blocking with Any with system.System, Any, Consumer with pipeType]] = for {
    layer <- ZIO.succeed(getLayer)
    kLayer <- KafkaConfig.kafkaConsumer.provideLayer(layer)
  } yield kLayer ++ layer

  def PIPE: ZIO[Consumer with pipeType, Throwable, Unit] = Pipeline.apply[
    Has[KafkaConfig],
    Consumer with Clock,
    Consumer with Clock with Has[ClickhouseConfig],
    ConsumerRecord[String, Array[Byte]],
    ClickhouseModel,
  ]

  def main: ZIO[Clock with Blocking with Any with system.System, Nothing, Unit] = (for {
    l <- layerEffect
    _ <- PIPE.provideLayer(l)
  } yield ()).catchAll(_ => ZIO.unit)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = main.as(ExitCode.success)
}