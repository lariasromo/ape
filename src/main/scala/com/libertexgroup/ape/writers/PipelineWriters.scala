package com.libertexgroup.ape.writers

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.libertexgroup.ape
import com.libertexgroup.ape.Writer
import com.libertexgroup.configs._
import com.libertexgroup.models.cassandra.CassandraModel
import com.libertexgroup.models.clickhouse.ClickhouseModel
import com.libertexgroup.models.jdbc.JDBCModel
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.Producer
import zio.s3.S3
import zio.{Chunk, Duration, Queue}

import scala.reflect.ClassTag

class PipelineWriters() {
  def queueWriter[E, ZE, T: ClassTag](queue:Queue[T]): Writer[E, ZE, T, T] = new ape.writers.QueueWriter[E, ZE, T](queue)

  // Writers
  def clickhouseWriter[ET]: Writer[MultiClickhouseConfig, ET, ClickhouseModel, Chunk[ClickhouseModel]] =
    new ape.writers.clickhouse.DefaultWriter[ET]

  def jDBCWriter[ET]: Writer[JDBCConfig, ET, JDBCModel, Chunk[JDBCModel]] =
    new ape.writers.jdbc.DefaultWriter[ET]

  def kafkaStringWriter[ET]: Writer[KafkaConfig, Producer with ET, ProducerRecord[String, String], ProducerRecord[String, String]] =
    new ape.writers.kafka.DefaultWriter[ET]

  def kafkaAvroWriter[ET, T: SchemaFor : Encoder]:
  Writer[KafkaConfig, Producer with ET, ProducerRecord[String, T], ProducerRecord[String, T]] =
    new ape.writers.kafka.AvroWriter[ET, T]

  def s3AvroWriter[ET, T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
  Writer[ET with S3 with S3Config, ET, T, T] = new ape.writers.s3.AvroWriter[ET, T]

  def s3ParquetWriter[ET, T >: Null : SchemaFor : Encoder : Decoder : ClassTag](chunkSize: Int, duration: Duration):
  Writer[ET with S3 with S3Config, ET, T, T] = new ape.writers.s3.ParquetWriter[ET, T](chunkSize, duration)

  def s3TextWriter[ET]: Writer[ET with S3 with S3Config, ET, String, String] = new ape.writers.s3.TextWriter[ET]

  def s3JsonLinesWriter[ET, T: ClassTag](implicit e: T => String): Writer[ET with S3 with S3Config, ET, T, T] =
    new ape.writers.s3.JsonLinesWriter[ET, T]

  def s3JsonLinesCirceWriter[ET, T: circe.Encoder: ClassTag]: Writer[ET with S3 with S3Config, ET, T, T] =
    new ape.writers.s3.JsonLinesCirceWriter[ET, T]

  def cassandraWriter[E]: Writer[CassandraConfig, E, CassandraModel, Chunk[AsyncResultSet]] =
    new ape.writers.cassandra.DefaultWriter[E]

  def consoleWriter[E, T: ClassTag]: Writer[E, E, T, T] = new ape.writers.ConsoleWriter[E, T]

  def consoleStringWriter[E]: Writer[E, E, String, String] = new ape.writers.ConsoleWriter[E, String]
}
