package com.libertexgroup.ape.readers

import com.libertexgroup.ape
import com.libertexgroup.ape.readers.s3.{S3FileReaderService, S3FileWithContent}
import com.libertexgroup.configs._
import com.libertexgroup.models.s3.KafkaRecordS3
import com.libertexgroup.models.websocket.Message
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import sttp.ws.WebSocket
import zio.kafka.consumer.Consumer
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream


import zio.{Task, ZIO}

import java.sql.ResultSet
import java.time.ZonedDateTime

import scala.reflect.ClassTag

// Readers
class PipelineReaders() {


  def restApiReaderByte() = new ape.readers.rest.RestAPIReaderByte

  def restApiReaderString() = new ape.readers.rest.RestAPIReaderString


  def noOpReader[E, T: ClassTag](stream: ZStream[Any, Throwable, T]) = new ape.readers.NoOpReader[E, T](stream)

  def clickhouseDefaultReader[T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[MultiClickhouseConfig, Any, T] =
    new ape.readers.clickhouse.DefaultReader[Any, T](sql)

  def jdbcDefaultReader[T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[JDBCConfig, Any, T] =
    new ape.readers.jdbc.DefaultReader[Any, T](sql)

  def kafkaDefaultReader: Reader[KafkaConfig, Consumer, ConsumerRecord[String, Array[Byte]]] =
    new ape.readers.kafka.DefaultReader()

  def kafkaAvroReader[T >:Null :SchemaFor :Decoder :Encoder]: Reader[KafkaConfig, Consumer, ConsumerRecord[String, Option[T]]] =
    new ape.readers.kafka.AvroReader[T]()

  def kafkaStringReader: Reader[KafkaConfig, Consumer, ConsumerRecord[String, String]] =
    new ape.readers.kafka.StringReader()

  def kafkaJsonCirceReader[T >:Null :io.circe.Decoder : ClassTag]: Reader[KafkaConfig, Consumer, ConsumerRecord[String, T]] =
    new ape.readers.kafka.JsonCirceReader[T]()

  def s3ParquetReader: Reader[S3FileReaderService, S3Config with S3, GenericRecord] =
    new ape.readers.s3.ParquetReader()

  def s3AvroReader[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
  Reader[S3FileReaderService with S3 with S3Config, S3 with S3Config, S3FileWithContent[T]] = new ape.readers.s3.AvroReader[T]

  def s3TextReader: Reader[S3FileReaderService with S3Config, S3Config with S3, S3FileWithContent[String]] =
    new ape.readers.s3.TextReader

  def s3FileReaderContinuous(lp:ZIO[S3Config, Nothing, ZonedDateTime => List[String]]):
  Reader[S3Config, S3, S3ObjectSummary] = new ape.readers.s3.FileReaderContinuous(lp)

  def s3FileReaderSimple(lp:ZIO[S3 with S3Config, Nothing, ZonedDateTime => List[String]]):
  Reader[S3 with S3Config, Any, S3ObjectSummary] = new ape.readers.s3.FileReaderSimple(lp)

  def s3TypedParquetReader[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
  Reader[S3FileReaderService with S3Config, S3Config with S3, S3FileWithContent[T]] = new ape.readers.s3.TypedParquetReader[T]

  def s3JsonLinesReader[T >: Null :ClassTag]()(implicit e: String => T):
  Reader[S3FileReaderService with S3Config, S3 with S3Config, S3FileWithContent[T]] = new ape.readers.s3.JsonLinesReader[T]

  def s3JsonLinesCirceReader[T >: Null :ClassTag :circe.Decoder]:
  Reader[S3FileReaderService with S3Config, S3 with S3Config, S3FileWithContent[T]] =
    new ape.readers.s3.JsonLinesCirceReader[T]

  def s3LbxLogstashKafkaReader: Reader[S3FileReaderService with S3Config, S3 with S3Config, S3FileWithContent[KafkaRecordS3]] =
    s3JsonLinesReader[KafkaRecordS3]

  def websocketReader(ws: WebSocket[Task]): Reader[Any, Any, Message] =
    new ape.readers.websocket.DefaultReader(ws)
}
