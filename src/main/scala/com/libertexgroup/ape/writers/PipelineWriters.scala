package com.libertexgroup.ape.writers

import com.libertexgroup.ape
import com.libertexgroup.configs._
import com.libertexgroup.models.cassandra.CassandraModel
import com.libertexgroup.models.clickhouse.ClickhouseModel
import com.libertexgroup.models.jdbc.JDBCModel
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.Producer
import zio.s3.S3
import zio.{Duration, Queue, Scope}

import scala.reflect.ClassTag

class PipelineWriters() {
  def queueWriter[E, T: ClassTag](queue:Queue[T]): Writer[E, E, T] = new ape.writers.QueueWriter[E, T](queue)

  // Writers
  def clickhouseWriter[E]: Writer[E, E with Scope with MultiClickhouseConfig, ClickhouseModel] =
    new ape.writers.clickhouse.DefaultWriter[E]

  def jDBCWriter[E]: Writer[E, E with Scope with JDBCConfig, JDBCModel] =
    new ape.writers.jdbc.DefaultWriter[E]

//  KafkaWriter[E, E with Producer with KafkaConfig with Scope, String, String]
//  Writer[E, E with Producer with KafkaConfig with Scope, ProducerRecord[String, String]]
  def kafkaStringWriter[E]: Writer[E, E with Producer with KafkaConfig with Scope, ProducerRecord[String, String]] =
    new ape.writers.kafka.DefaultWriter[E]

  def kafkaAvroWriter[E, T: SchemaFor : Encoder]: Writer[E, E with Producer with KafkaConfig with Scope, ProducerRecord[String, T]] =
    new ape.writers.kafka.AvroWriter[E, T]

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

  def cassandraWriter[E]: Writer[E, E with Scope with CassandraConfig, CassandraModel] =
    new ape.writers.cassandra.DefaultWriter[E]

  def consoleWriter[E, T]: Writer[E, E with Scope, T] = new ape.writers.ConsoleWriter[E, T]

  def consoleStringWriter[E]: Writer[E, E with Scope, String] = new ape.writers.ConsoleWriter[E, String]
}
