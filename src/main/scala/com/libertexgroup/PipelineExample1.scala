package com.libertexgroup

import com.libertexgroup.algebras.pipelines.Pipeline
import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.algebras.readers.kafka.KafkaDefaultReader
import com.libertexgroup.algebras.readers.s3.S3DefaultReader
import com.libertexgroup.algebras.transformers.{GenericRecord2StringTransformer, NoOpTransformer, Transformer}
import com.libertexgroup.algebras.transformers.clickhouse.DefaultClickhouseTransformer
import com.libertexgroup.algebras.writers.{DefaultWriter, Writer, clickhouse}
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig, S3Config}
import com.libertexgroup.models.ClickhouseModel
import com.libertexgroup.models.EncodingType.PARQUET
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.kafka.consumer.Consumer
import zio.s3.S3
import zio.{ExitCode, Has, URIO, ZIO, ZLayer, system}

object PipelineExample1 extends zio.App {
  val readText: ZIO[Console with S3 with Has[S3Config], Throwable, Unit] = {
    val reader = new S3DefaultReader()
    val transformer: Transformer[S3, GenericRecord, GenericRecord] = new NoOpTransformer[S3, GenericRecord]()
    val writer: Writer[S3, Console with S3, GenericRecord] = new DefaultWriter[S3, GenericRecord]()
    (for {
      _ <- putStrLn("hello")
      _ <- Pipeline.createWithETL(
        reader, transformer, writer
      )
    } yield ())
  }

  val awsAccessKey = "..."
  val awsSecretKey = "..."
  val s3Config = S3Config(
    location = Some("parquet/anodot/table=clientPrediction/year=2023/month=01/day=23/"),
    s3Bucket = Some("eu-west-1-datalake-prod"),
    s3Host = "https://s3.eu-west-1.amazonaws.com",
    encodingType = PARQUET,
    parallelism = 3,
    awsAccessKey = awsAccessKey,
    awsSecretKey = awsSecretKey,
  )

  val s3Layer: ZLayer[Any, S3Exception, S3 with Has[S3Config] with Console] =
    zio.s3.liveM(Region.EU_WEST_1, zio.s3.providers.const(awsAccessKey, awsSecretKey)) ++
      ZLayer.succeed(s3Config) ++ zio.console.Console.live

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = readText.provideLayer(s3Layer).orDie.as(ExitCode.success)
}