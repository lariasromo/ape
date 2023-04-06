package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.Writer
import com.libertexgroup.configs._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import purecsv.safe.converter.Converter
import zio.s3.S3
import zio.{Duration, Tag}

import scala.reflect.ClassTag

protected[writers] class Writers[Config <: S3Config :Tag, AWSS3 <: S3 :Tag]() {
  def avro[ET, T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
  Writer[ET with AWSS3 with Config, ET, T, T] = new AvroWriter[ET, T, Config, AWSS3]

  def parquet[ET,
    T >: Null : SchemaFor : Encoder : Decoder : ClassTag
  ](chunkSize: Int, duration: Duration): Writer[ET with AWSS3 with Config, ET, T, T] =
    new ParquetWriter[ET, T, Config, AWSS3](chunkSize, duration)

  def csv[ET, T: ClassTag](sep: String = ",")
                                (implicit rfc: Converter[T,Seq[String]]): Writer[ET with AWSS3 with Config, ET, T, T] =
    new CsvWriter[ET, T, Config, AWSS3](sep)

  def text[ET]: Writer[ET with AWSS3 with Config, ET, String, String] =
    new TextWriter[ET, Config, AWSS3]

  def jsonLines[ET, T: ClassTag](implicit e: T => String): Writer[ET with AWSS3 with Config, ET, T, T] =
    new JsonLinesWriter[ET, T, Config, AWSS3]

  def jsonLinesCirce[ET, T: circe.Encoder: ClassTag]: Writer[ET with AWSS3 with Config, ET, T, T] =
    new JsonLinesCirceWriter[ET, T, Config, AWSS3]
}
