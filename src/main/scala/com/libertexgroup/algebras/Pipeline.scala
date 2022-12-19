package com.libertexgroup.algebras

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.algebras.readers.kafka.KafkaDefaultReader
import com.libertexgroup.algebras.readers.s3.S3DefaultReader
import com.libertexgroup.algebras.transformers.DefaultTransformer
import com.libertexgroup.algebras.transformers.clickhouse.DefaultClickhouseTransformer
import com.libertexgroup.algebras.writers.clickhouse.DefaultWriter
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig, S3Config}
import zio.clock.Clock
import zio.kafka.consumer.Consumer
import zio.{Has, ZIO}
import zio.s3.S3
import zio.stream.ZStream

object Pipeline {
  val reader = "KafkaDefault"
  val transformer = "default"
  val writer = "default"
  val readerI: ZIO[Any with Consumer with Clock with Has[ClickhouseConfig] with Has[KafkaConfig] with S3 with Has[S3Config], Any, Unit] =
    reader match {
      case "KafkaDefault" => apply(KafkaDefaultReader)
      case "S3Default" => apply(S3DefaultReader)
    }

  def apply(readerI: Reader): ZIO[readerI.Env2 with Has[ClickhouseConfig] with readerI.Env, Any, Unit] =
    for {
      stream <- readerI.apply
      transformedStream = transformer match {
        case "default" => new DefaultClickhouseTransformer[readerI.Env2, readerI.StreamType].apply(stream)
      }
      _ <- writer match {
        case "default" => new DefaultWriter[readerI.Env2].apply(transformedStream)
      }
    } yield ()
}
