package com.libertexgroup.ape.writers.s3.fromS3Files

import com.libertexgroup.ape.{Ape, Reader}
import com.libertexgroup.ape.readers.s3.{S3FileReader, S3FileWithContent}
import com.libertexgroup.configs._
import com.libertexgroup.models.s3.BackPressureType.BackPressureType
import com.libertexgroup.models.s3.KafkaRecordS3
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import zio.Tag
import zio.stream.ZStream

import scala.reflect.ClassTag

// Readers
class Writers[ZE, Config <: S3Config :Tag](reader: S3FileReader[Config, ZE]) {
  def avro[T >: Null : SchemaFor : Decoder : Encoder : ClassTag]: Reader[Any, ZE with Config, S3FileWithContent[T]] =
    Reader.unitReader[Any, ZE with Config, S3FileWithContent[T]](reader --> new AvroPipe[ZE, T, Config])

  def text: Reader[Any, ZE with Config, S3FileWithContent[String]] =
      Reader.unitReader[Any, ZE with Config, S3FileWithContent[String]](reader --> new TextPipe[ZE, Config])

  def jsonLines[T >: Null :ClassTag]()(implicit e: String => T): Reader[Any, ZE with Config, S3FileWithContent[T]] =
    Reader.unitReader[Any, ZE with Config, S3FileWithContent[T]](reader --> new JsonLinesPipe[ZE, T, Config])

  def jsonLinesCirce[T >: Null :ClassTag :circe.Decoder]: Reader[Any, ZE with Config, S3FileWithContent[T]] =
    Reader.unitReader[Any, ZE with Config, S3FileWithContent[T]](reader --> new JsonLinesCircePipe[ZE, T, Config])

  def lbxLogstashKafka: Reader[Any, ZE with Config, S3FileWithContent[KafkaRecordS3]] = jsonLines[KafkaRecordS3]
}
