package com.libertexgroup.ape.writers.s3.fromData

import com.libertexgroup.ape.Writer
import com.libertexgroup.configs._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import zio.s3.S3
import zio.{Duration, Tag}

import scala.reflect.ClassTag

protected[writers] class Writers[Config <: S3Config :Tag]() {
    def avro[ET, T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
    Writer[ET with S3 with Config, ET, T, T] = new AvroWriter[ET, T, Config]

    def parquet[ET,
      T >: Null : SchemaFor : Encoder : Decoder : ClassTag
    ](chunkSize: Int, duration: Duration): Writer[ET with S3 with Config, ET, T, T] =
      new ParquetWriter[ET, T, Config](chunkSize, duration)

    def csv[ET, T: ClassTag](
                              sep: String = ",",
                              order:Option[Seq[String]]=None
                            ): Writer[ET with S3 with Config, ET, T, T] =
      new CsvWriter[ET, T, Config](sep, order)

    def text[ET]: Writer[ET with S3 with Config, ET, String, String] =
      new TextWriter[ET, Config]

    def jsonLines[ET, T: ClassTag](implicit e: T => String): Writer[ET with S3 with Config, ET, T, T] =
      new JsonLinesWriter[ET, T, Config]

    def jsonLinesCirce[ET, T: circe.Encoder: ClassTag]: Writer[ET with S3 with Config, ET, T, T] =
      new JsonLinesCirceWriter[ET, T, Config]
  }
