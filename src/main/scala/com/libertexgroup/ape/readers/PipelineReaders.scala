package com.libertexgroup.ape.readers

import com.libertexgroup.ape
import com.libertexgroup.configs.{ClickhouseConfig, JDBCConfig, KafkaConfig, S3Config}
import com.libertexgroup.models.websocket.Message
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import sttp.ws.WebSocket
import zio.Task
import zio.kafka.consumer.Consumer
import zio.s3.S3

import java.sql.ResultSet
import scala.reflect.ClassTag

// Readers
class PipelineReaders() {
  def clickhouseDefaultReader[T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[ClickhouseConfig, Any, T] =
    new ape.readers.clickhouse.DefaultReader[Any, T](sql)

  def jdbcDefaultReader[T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[JDBCConfig, Any, T] =
    new ape.readers.jdbc.DefaultReader[Any, T](sql)

  def kafkaDefaultReader: Reader[KafkaConfig, Consumer, ConsumerRecord[String, Array[Byte]]] =
    new ape.readers.kafka.DefaultReader()

  def kafkaAvroReader[T >:Null :SchemaFor :Decoder :Encoder]: Reader[KafkaConfig, Consumer, ConsumerRecord[String, Option[T]]] =
    new ape.readers.kafka.AvroReader[T]()

  def kafkaStringReader: Reader[KafkaConfig, Consumer, ConsumerRecord[String, String]] =
    new ape.readers.kafka.StringReader()

  def s3ParquetReader: Reader[S3 with S3Config, S3, GenericRecord] = new ape.readers.s3.ParquetReader()

  def s3AvroReader[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]: Reader[S3 with S3Config, S3, T] =
    new ape.readers.s3.AvroReader[T]()

  def s3TextReader: Reader[S3 with S3Config, S3, String] = new ape.readers.s3.TextReader()

  def s3TypedParquetReader[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]: Reader[S3 with S3Config, S3, T] =
    new ape.readers.s3.TypedParquetReader[T]()

  def s3JsonLinesReader[T >: Null :ClassTag](implicit e: String => T): Reader[S3 with S3Config, S3, T] =
    new ape.readers.s3.JsonLinesReader[T]()

  def s3JsonLinesCirceReader[T >: Null :ClassTag :circe.Decoder]: Reader[S3 with S3Config, S3, T] =
    new ape.readers.s3.JsonLinesCirceReader[T]()

  def websocketReader(ws: WebSocket[Task]): Reader[Any, Any, Message] =
    new ape.readers.websocket.DefaultReader(ws)
}
