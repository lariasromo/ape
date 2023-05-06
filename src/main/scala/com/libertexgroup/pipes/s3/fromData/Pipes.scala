package com.libertexgroup.pipes.s3.fromData

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe
import zio.s3.S3
import zio.{Duration, Tag}

import scala.reflect.ClassTag

protected[pipes] class Pipes[Config <: S3Config :Tag]() {
    def avro[ET, T >: Null : SchemaFor : Decoder : Encoder : ClassTag]:
    Pipe[ET with S3 with Config, ET, T, T] = new AvroPipe[ET, T, Config]

    def parquet[ET,
      T >: Null : SchemaFor : Encoder : Decoder : ClassTag
    ](chunkSize: Int, duration: Duration): Pipe[ET with S3 with Config, ET, T, T] =
      new ParquetPipe[ET, T, Config](chunkSize, duration)

    def csv[ET, T: ClassTag](
                              sep: String = ",",
                              order:Option[Seq[String]]=None
                            ): Pipe[ET with S3 with Config, ET, T, T] =
      new CsvPipe[ET, T, Config](sep, order)

    def text[ET]: Pipe[ET with S3 with Config, ET, String, String] =
      new TextPipe[ET, Config]

    def jsonLines[ET, T: ClassTag](implicit e: T => String): Pipe[ET with S3 with Config, ET, T, T] =
      new JsonLinesPipe[ET, T, Config]

    def jsonLinesCirce[ET, T: circe.Encoder: ClassTag]: Pipe[ET with S3 with Config, ET, T, T] =
      new JsonLinesCircePipe[ET, T, Config]
  }
