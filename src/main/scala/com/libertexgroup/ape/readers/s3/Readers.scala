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
protected [readers] class Readers[Config <: S3Config :Tag, AWSS3 <: S3 :Tag]() {
  def parquet[E]: Reader[S3FileReaderService[Config, AWSS3] with E, Config with AWSS3, GenericRecord] =
    new ParquetReader[E, AWSS3, Config]()

  def avro[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
    Reader[
      S3FileReaderService[Config, AWSS3] with AWSS3 with Config,
      AWSS3 with Config,
      S3FileWithContent[T, AWSS3]
    ] = new AvroReader[T, Config, AWSS3]

  def text: Reader[S3FileReaderService[Config, AWSS3] with Config, Config with AWSS3, S3FileWithContent[String, AWSS3]] =
    new TextReader[Config, AWSS3]

  def fileReaderContinuous(lp:ZIO[Config, Nothing, ZonedDateTime => List[String]]): Reader[Config, AWSS3, S3ObjectSummary] =
    new FileReaderContinuous(lp)

  def fileReaderSimple(lp:ZIO[AWSS3 with Config, Nothing, ZonedDateTime => List[String]]):
  Reader[AWSS3 with Config, Any, S3ObjectSummary] = new FileReaderSimple[Config, AWSS3](lp)

  def fileReaderBounded(locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
                              start:ZonedDateTime, end:ZonedDateTime, step:Duration):
  Reader[AWSS3 with Config, Any, S3ObjectSummary] =
    new FileReaderBounded[Config, AWSS3](locationPattern, start, end, step)

  def typedParquet[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
  Reader[S3FileReaderService[Config, AWSS3] with Config, Config with AWSS3, S3FileWithContent[T, AWSS3]] =
    new TypedParquetReader[T, Config, AWSS3]

  def jsonLines[T >: Null :ClassTag]()(implicit e: String => T):
  Reader[S3FileReaderService[Config, AWSS3] with Config, AWSS3 with Config, S3FileWithContent[T, AWSS3]] =
    new JsonLinesReader[T, Config, AWSS3]

  def jsonLinesCirce[T >: Null :ClassTag :circe.Decoder]:
  Reader[S3FileReaderService[Config, AWSS3] with Config, AWSS3 with Config, S3FileWithContent[T, AWSS3]] =
    new JsonLinesCirceReader[T, Config, AWSS3]

  def lbxLogstashKafka: Reader[S3FileReaderService[Config, AWSS3] with Config, AWSS3 with Config,
    S3FileWithContent[KafkaRecordS3, AWSS3]] = jsonLines[KafkaRecordS3]
}
