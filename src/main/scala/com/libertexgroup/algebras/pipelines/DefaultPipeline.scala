package com.libertexgroup.algebras.pipelines

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.algebras.readers.kafka.KafkaDefaultReader
import com.libertexgroup.algebras.readers.s3.S3DefaultReader
import com.libertexgroup.algebras.transformers.clickhouse.DefaultClickhouseTransformer
import com.libertexgroup.algebras.writers.clickhouse.DefaultWriter
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig, ProgramConfig, S3Config}
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.kafka.consumer.Consumer
import zio.s3.S3
import zio.{Has, Schedule, ZIO, duration}

import java.util.concurrent.TimeUnit

object DefaultPipeline {
  def apply(readerI: Reader): ZIO[Console with readerI.Env2 with Has[ClickhouseConfig] with readerI.Env with Has[ProgramConfig], Throwable, Unit] =
    for {
      config <- ZIO.access[Has[ProgramConfig]](_.get)

      stream <- readerI.apply
      transformedStream = config.transformer match {
        // register more transformers here
        case "default" => new DefaultClickhouseTransformer[readerI.Env2, readerI.StreamType].apply(stream)
        case _ => throw new Exception(s"Transformer: ${config.transformer} is not supported")
      }
      _ <- (config.writer match {
        // register more writers here
        case "ClickhouseDefault" => new DefaultWriter[readerI.Env2].apply(transformedStream)
        case _ => throw new Exception(s"Writer: ${config.writer} is not supported")
      }).catchAll(e => putStrLn(e.toString))
    } yield ()

  type AllConfigs = Has[ClickhouseConfig] with Has[KafkaConfig] with Has[ProgramConfig] with S3 with Has[S3Config]
  def getReader(config: ProgramConfig): ZIO[Console with Consumer with Clock with AllConfigs, Throwable, Unit] = config.reader match {
    // register more readers here
    case "KafkaDefault" => apply(KafkaDefaultReader)
    case "S3Default" => apply(S3DefaultReader)
    case _ => throw new Exception(s"Reader: ${config.reader} is not supported")
  }

  def run: ZIO[Console with Consumer with Clock with AllConfigs with S3, Throwable, AnyVal] = for {
    config <- ZIO.access[Has[ProgramConfig]](_.get)
    pipe <- {
      getReader(config).repeat {
        config.streamConfig.map(streamConfig => {
          Schedule.spaced {
            duration.Duration.apply(streamConfig.durationMinutes, TimeUnit.MINUTES)
          }
        }).getOrElse(Schedule.once)
      }
    }
  } yield pipe
}
