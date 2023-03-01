package com.libertexgroup.ape.writers

import com.libertexgroup.ape
import com.libertexgroup.configs.{ClickhouseConfig, JDBCConfig, KafkaConfig, S3Config}
import com.libertexgroup.models.{ClickhouseModel, JDBCModel}
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.Producer
import zio.s3.S3
import zio.{Console, Duration, Scope}

import scala.reflect.ClassTag

class PipelineWriters() {
  // Writers
  def clickhouseWriter: Writer[Any, Any with Scope with ClickhouseConfig, ClickhouseModel] =
    new ape.writers.clickhouse.DefaultWriter[Any]

  def jDBCWriter: Writer[Any, Any with Scope with JDBCConfig, JDBCModel] =
    new ape.writers.jdbc.DefaultWriter[Any]

  def kafkaStringWriter: Writer[Any, Any with Producer with KafkaConfig, ProducerRecord[String, String]] =
    new ape.writers.kafka.DefaultWriter[Any]

  def kafkaAvroWriter[T: SchemaFor : Encoder]: Writer[Any, Any with Producer with KafkaConfig, ProducerRecord[String, T]] =
    new ape.writers.kafka.AvroWriter[Any, T]

  def s3AvroWriter[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]: Writer[Any, Any with S3 with S3Config, T] =
    new ape.writers.s3.AvroWriter[Any, T]

  def s3ParquetWriter[T >: Null : SchemaFor : Encoder : Decoder : ClassTag]
  (chunkSize: Int, duration: Duration): Writer[Any, Any with S3 with S3Config, T] =
    new ape.writers.s3.ParquetWriter[Any, T](chunkSize, duration)

  def s3TextWriter: Writer[Any, Any with S3 with S3Config, String] = new ape.writers.s3.TextWriter[Any]

  def s3JsonLinesWriter[T](implicit e: T => String): Writer[Any, Any with S3 with S3Config, T] =
    new ape.writers.s3.JsonLinesWriter[Any, T]

  def s3JsonLinesCirceWriter[T: circe.Encoder]: Writer[Any, Any with S3 with S3Config, T] =
    new ape.writers.s3.JsonLinesCirceWriter[Any, T]

  def consoleWriter[E, T]: Writer[E, E, T] = new ape.writers.ConsoleWriter[E, T]

  def consoleStringWriter[E]: Writer[E, E, String] = new ape.writers.ConsoleWriter[E, String]
}
