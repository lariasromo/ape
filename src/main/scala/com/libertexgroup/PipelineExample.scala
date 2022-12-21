package com.libertexgroup

import com.libertexgroup.algebras.pipelines.DefaultPipeline
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig, ProgramConfig, S3Config}
import software.amazon.awssdk.regions.Region
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.kafka.consumer.Consumer
import zio.s3.S3
import zio.{ExitCode, Has, URIO, ZIO, ZLayer, system}

object PipelineExample extends zio.App {
  type ConsumerEnv = Consumer with Env
  type Env = Clock with S3 with Console
    with Has[ClickhouseConfig]
    with Has[KafkaConfig]
    with Has[S3Config]

  val configJson: String = """
                     |{
                     |    "reader": "KafkaDefault",
                     |    "transformer": "default",
                     |    "writer": "ClickhouseDefault"
                     |}
                     |""".stripMargin

  val layer: ZLayer[system.System, Throwable, Env] = Console.live ++ Clock.live ++
    zio.s3.liveM(Region.EU_WEST_1, zio.s3.providers.system <> zio.s3.providers.env) ++
    ClickhouseConfig.live ++ KafkaConfig.live ++ S3Config.live


  def main: ZIO[Clock with Blocking with system.System with Console, Throwable, Unit] = for {
    kLayer <- KafkaConfig.kafkaConsumer.provideLayer(layer)
    _ <- DefaultPipeline.run.provideLayer(layer ++ kLayer ++ ProgramConfig.fromJsonString(configJson))
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = main.orDie.as(ExitCode.success)
}