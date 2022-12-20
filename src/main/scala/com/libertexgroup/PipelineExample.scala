package com.libertexgroup

import com.libertexgroup.algebras.pipelines.DefaultPipeline
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig, ProgramConfig, S3Config}
import software.amazon.awssdk.regions.Region
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.kafka.consumer.diagnostics.Diagnostics
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.s3.S3
import zio.{ExitCode, Has, URIO, ZIO, ZLayer, system}

object PipelineExample extends zio.App {
  type Env = Consumer with Clock with S3 with Console
    with Has[ClickhouseConfig]
    with Has[KafkaConfig]
    with Has[ProgramConfig]
    with Has[S3Config]


  val layer: ZLayer[Clock with Blocking with Any with system.System, Throwable, Env] =
    ZLayer.fromManaged(
      Consumer.make(
        ConsumerSettings(List("localhost:29092")).withGroupId("group")
      )
    ) ++ Console.live ++ Clock.live ++
      zio.s3.liveM(Region.EU_WEST_1, zio.s3.providers.system <> zio.s3.providers.env) ++
      ClickhouseConfig.live ++ KafkaConfig.live ++ ProgramConfig.live ++ S3Config.live

  def main: ZIO[Env, Throwable, Unit] = for {
    _ <- putStrLn("Hello world")
    _ <- DefaultPipeline.run
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = main
    .provideLayer(layer)
    .orDie
    .as(ExitCode.success)
}