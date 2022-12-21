import com.libertexgroup.PipelineExample.{ConsumerEnv, Env}
import com.libertexgroup.algebras.pipelines.DefaultPipeline
import com.libertexgroup.configs._
import services.S3ClickhouseContainerService.ClickhouseS3
import services.{KafkaContainerService, S3ClickhouseContainerService}
import software.amazon.awssdk.regions.Region
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.test.{Assert, DefaultRunnableSpec, ZSpec, assertTrue}
import zio.{ZIO, ZLayer, system}

import java.net.URI

object PipelineTest extends DefaultRunnableSpec {
  val configLayer: ZLayer[Blocking with system.System with ClickhouseS3, Throwable, Env] =
    zio.console.Console.live ++
    zio.s3.liveM( Region.EU_WEST_1,
      zio.s3.providers.const("minio", "minio123"),
      Some(URI.create(s"http://localhost:9001"))
    ) ++
    Clock.live ++
    Blocking.live ++
    KafkaContainerService.kafkaConfigLayer ++
    S3ClickhouseContainerService.live ++
    ProgramConfig.fromJsonString(
      """
        |{
        |    "reader": "KafkaDefault",
        |    "transformer": "default",
        |    "writer": "ClickhouseDefault"
        |}
        |""".stripMargin)

  val layerEffect: ZIO[Blocking with system.System with ClickhouseS3, Throwable,
    ZLayer[Clock with Blocking with system.System with ClickhouseS3, Throwable, ConsumerEnv]] = for {
    layer <- ZIO.succeed(configLayer)
    kLayer <- KafkaConfig.kafkaConsumer.provideLayer(layer)
  } yield kLayer ++ layer

  val test: ZIO[Console with Clock with Blocking with system.System with ClickhouseS3, Throwable, Assert] = for {
    l <- layerEffect
    testResult <- ZIO.succeed(true)
    _ <- DefaultPipeline.run
      .provideLayer(l)
      .catchAll(e => putStrLn(e.getMessage))
  } yield {
    assertTrue(testResult)
  }

  val preLayer: ZLayer[Environment, Nothing, Console with Clock with Blocking with system.System with ClickhouseS3] =
    Console.live ++ Clock.live ++ Blocking.live ++ system.System.live ++ S3ClickhouseContainerService.clickhouse

  override def spec: ZSpec[Environment, Failure] = suite("PipelineTest")(
    testM("Hello world") {
      test
    }.provideLayer(preLayer)
  )
}
