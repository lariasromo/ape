package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.ZIO
import zio.s3.S3
import zio.stream.ZStream

import scala.reflect.ClassTag


/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[readers] class TypedParquetReader[T >:Null: SchemaFor :Encoder :Decoder :ClassTag](location:String)
  extends S3Reader[S3 with S3Config, S3, T] {
  override def apply: ZIO[S3 with S3Config, Throwable, ZStream[S3, Throwable, T]] = readParquet[T](location)
}
