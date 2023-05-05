package com.libertexgroup.ape.writers.s3.fromS3Files.noneBacked

import com.libertexgroup.ape.readers.s3.S3FileWithContent
import com.libertexgroup.ape.writers.s3.fromS3Files._
import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class AvroPipe[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag,
  Config <: S3Config :Tag
] extends S3ContentPipe[ Config, Any, T ] {
  override protected[this] def pipe(i: ZStream[Any, Throwable, S3ObjectSummary]):
    ZIO[Config, Throwable, ZStream[Any, Throwable, S3FileWithContent[T]]] = S3FilePipe.avroPipe(i)
}
