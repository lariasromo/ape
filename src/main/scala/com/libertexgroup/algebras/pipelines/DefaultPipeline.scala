package com.libertexgroup.algebras.pipelines

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.algebras.readers.kafka.KafkaDefaultReader
import com.libertexgroup.algebras.readers.s3.S3DefaultReader
import com.libertexgroup.algebras.transformers.clickhouse.DefaultClickhouseTransformer
import com.libertexgroup.algebras.writers.clickhouse.DefaultWriter
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig, ProgramConfig, S3Config}
import zio.clock.Clock
import zio.kafka.consumer.Consumer
import zio.s3.S3
import zio.{Has, Schedule, ZIO, duration}

import java.util.concurrent.TimeUnit

object DefaultPipeline {
  def apply(readerI: Reader): ZIO[readerI.Env2 with Has[ClickhouseConfig] with readerI.Env with Has[ProgramConfig], Any, Unit] =
    for {
      config <- ZIO.access[Has[ProgramConfig]](_.get)

      stream <- readerI.apply
      transformedStream = config.transformer match {
        // register more transformers here
        case "default" => new DefaultClickhouseTransformer[readerI.Env2, readerI.StreamType].apply(stream)
      }
      _ <- config.writer match {
        // register more writers here
        case "default" => new DefaultWriter[readerI.Env2].apply(transformedStream)
      }
    } yield ()


  def run: ZIO[
    Consumer with Clock with S3
      with Has[ClickhouseConfig]
      with Has[KafkaConfig]
      with Has[ProgramConfig]
      with Has[S3Config],
    Throwable, Unit] = for {
    config <- ZIO.access[Has[ProgramConfig]](_.get)
    pipe = {
      (
        config.reader match {
          // register more readers here
          case "KafkaDefault" => apply(KafkaDefaultReader)
          case "S3Default" => apply(S3DefaultReader)
        }
      ).repeat {
        config.streamConfig.map(streamConfig => {
          Schedule.spaced {
            duration.Duration.apply(streamConfig.durationMinutes, TimeUnit.MINUTES)
          }
        }).getOrElse(Schedule.once)
      }
    }
  } yield pipe
}
