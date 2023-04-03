package com.libertexgroup.ape.readers

import com.libertexgroup.ape
import com.libertexgroup.ape.readers.s3.{S3FileReaderService, S3FileWithContent}
import com.libertexgroup.ape.Reader
import com.libertexgroup.ape.Reader.UnitReader
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
<<<<<<< HEAD
import zio.{Task, ZIO}
=======
import zio.{Duration, Task, ZIO}

>>>>>>> master
import java.sql.ResultSet
import java.time.ZonedDateTime

import zio.http.Request

import scala.reflect.ClassTag

// Readers
class PipelineReaders() {



  def restApiReaderByte[E](request: Request) = new ape.readers.rest.RestAPIReaderByte[E](request)

  def restApiReaderString[E](request: Request) = new ape.readers.rest.RestAPIReaderString[E](request)



  def noOpReader[E, ZE, T: ClassTag](stream: ZStream[ZE, Throwable, T]): Reader[Any, ZE, T] = new UnitReader(stream)

  def clickhouseDefaultReader[ET, T: ClassTag](sql: String)(implicit r2o: ResultSet => T):
  Reader[MultiClickhouseConfig, ET, T] = new ape.readers.clickhouse.DefaultReader[ET, T](sql)


  def jdbcDefaultReader[ET, T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[JDBCConfig, ET, T] =
    new ape.readers.jdbc.DefaultReader[ET, T](sql)

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

  def s3FileReaderSimple[ET](lp:ZIO[S3 with S3Config, Nothing, ZonedDateTime => List[String]]):
  Reader[S3 with S3Config, Any, S3ObjectSummary] = new ape.readers.s3.FileReaderSimple(lp)

  def s3FileReaderBounded[ET](locationPattern:ZIO[S3Config, Nothing, ZonedDateTime => List[String]],
                              start:ZonedDateTime, end:ZonedDateTime, step:Duration):
  Reader[S3 with S3Config, Any, S3ObjectSummary] = new ape.readers.s3.FileReaderBounded(locationPattern, start, end, step)

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
