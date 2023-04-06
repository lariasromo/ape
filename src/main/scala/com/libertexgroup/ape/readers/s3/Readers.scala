package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader
import com.libertexgroup.configs._
import com.libertexgroup.models.s3.KafkaRecordS3
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import org.apache.avro.generic.GenericRecord
import zio.s3.{S3, S3ObjectSummary}
import zio.{Duration, Tag, ZIO}

import java.time.ZonedDateTime
import scala.reflect.ClassTag

// Readers
protected [readers] class Readers[Config <: S3Config :Tag]() {
  def parquet[E]: Reader[S3FileReaderService[Config] with E, Config with S3, GenericRecord] =
    new ParquetReader[E, Config]()

  def avro[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]: 
  Reader[
    S3FileReaderService[Config] with Config,
    S3 with Config,
    S3FileWithContent[T]
  ] = new AvroReader[T, Config]

  def text: Reader[S3FileReaderService[Config] with Config, Config with S3, S3FileWithContent[String]] =
    new TextReader[Config]

  def fileReaderContinuous(lp:ZIO[Config, Nothing, ZonedDateTime => List[String]]): Reader[Config, S3, S3ObjectSummary] =
    new FileReaderContinuous(lp)

  def fileReaderSimple(lp:ZIO[S3 with Config, Nothing, ZonedDateTime => List[String]]):
  Reader[S3 with Config, Any, S3ObjectSummary] = new FileReaderSimple[Config, S3](lp)

  def fileReaderBounded(locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
                              start:ZonedDateTime, end:ZonedDateTime, step:Duration):
  Reader[S3 with Config, Any, S3ObjectSummary] =
    new FileReaderBounded[Config, S3](locationPattern, start, end, step)

  def typedParquet[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
  Reader[S3FileReaderService[Config] with Config, Config with S3, S3FileWithContent[T]] =
    new TypedParquetReader[T, Config]

  def jsonLines[T >: Null :ClassTag]()(implicit e: String => T):
  Reader[S3FileReaderService[Config] with Config, S3 with Config, S3FileWithContent[T]] =
    new JsonLinesReader[T, Config]

  def jsonLinesCirce[T >: Null :ClassTag :circe.Decoder]:
  Reader[S3FileReaderService[Config] with Config, S3 with Config, S3FileWithContent[T]] =
    new JsonLinesCirceReader[T, Config]

  def lbxLogstashKafka: Reader[S3FileReaderService[Config] with Config, S3 with Config,
    S3FileWithContent[KafkaRecordS3]] = jsonLines[KafkaRecordS3]
}
