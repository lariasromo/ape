package com.libertexgroup.ape.readers

import com.libertexgroup.ape
import com.libertexgroup.ape.readers.s3.{LBXLogstashKafkaReader, S3Reader}
import com.libertexgroup.ape.utils.S3Utils.pathConverter
import com.libertexgroup.configs.{ClickhouseConfig, JDBCConfig, KafkaConfig, S3Config}
import com.libertexgroup.models.s3.KafkaRecordS3
import com.libertexgroup.models.websocket.Message
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import sttp.ws.WebSocket
import zio.{Duration, Task, ZIO}
import zio.kafka.consumer.Consumer
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream

import java.sql.ResultSet
import java.time.{LocalDateTime, ZonedDateTime}
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

  def kafkaJsonCirceReader[T >:Null :io.circe.Decoder : ClassTag]: Reader[KafkaConfig, Consumer, ConsumerRecord[String, T]] =
    new ape.readers.kafka.JsonCirceReader[T]()

  def s3ParquetReader(location:String): Reader[S3 with S3Config, S3, GenericRecord] =
    new ape.readers.s3.ParquetReader(location)

  def s3AvroReader[T >: Null : SchemaFor : Decoder : Encoder : ClassTag](location:String): Reader[S3 with S3Config, S3, T] =
    new ape.readers.s3.AvroReader[T](location)

  def s3TextReader(location:String): Reader[S3 with S3Config, S3, String] = new ape.readers.s3.TextReader(location)

  def s3TypedParquetReader[T >: Null : SchemaFor : Decoder : Encoder : ClassTag](location:String): Reader[S3 with S3Config, S3, T] =
    new ape.readers.s3.TypedParquetReader[T](location)

  def s3JsonLinesReader[T >: Null :ClassTag](location:String)(implicit e: String => T): Reader[S3 with S3Config, S3, T] =
    new ape.readers.s3.JsonLinesReader[T](location)

  def s3JsonLinesCirceReader[T >: Null :ClassTag :circe.Decoder](location:String): Reader[S3 with S3Config, S3, T] =
    new ape.readers.s3.JsonLinesCirceReader[T](location)

  def s3LbxLogstashKafkaReader(locationPattern:ZIO[S3Config, Nothing, ZonedDateTime => List[String]]):
  Reader[S3 with S3Config, S3 with S3Config,  (S3ObjectSummary, ZStream[S3, Throwable, KafkaRecordS3])] =
    new ape.readers.s3.LBXLogstashKafkaReader(locationPattern)

  def s3LbxLogstashWithPathConverterReader(location: String):
  Reader[S3 with S3Config, S3 with S3Config, (S3ObjectSummary, ZStream[S3, Throwable, KafkaRecordS3])] =
    new ape.readers.s3.LBXLogstashKafkaReader(pathConverter(location))

  def websocketReader(ws: WebSocket[Task]): Reader[Any, Any, Message] =
    new ape.readers.websocket.DefaultReader(ws)
}
