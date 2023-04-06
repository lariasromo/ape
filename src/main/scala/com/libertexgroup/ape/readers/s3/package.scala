package com.libertexgroup.ape.readers

import com.libertexgroup.ape.utils.ParquetUtils.{readParquetGenericRecord, readParquetWithType}
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.CompressionType._
import com.libertexgroup.models.s3.CompressionType
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.avro.generic.GenericRecord
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.Console.printLine
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.{ZPipeline, ZStream}
import zio.{Queue, ZIO}

import java.io.IOException

package object s3 {
  type S3FileWithContent[T] = (S3ObjectSummary, ZStream[S3, Throwable, T])

  def readPlainText(compressionType: CompressionType, file: S3ObjectSummary): ZStream[S3, Exception, String] =
  {
    val stream = zio.s3.getObject(file.bucketName, file.key)
    (compressionType match {
      case CompressionType.GZIP | CompressionType.GUNZIP => stream.via(ZPipeline.gunzip(64 * 1024))
      case NONE => stream
    }).via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
  }

  def decompressStream(compressionType: CompressionType,
                       stream: ZStream[S3, S3Exception, Byte]): ZStream[S3, Exception, Byte] =
    compressionType match {
      case CompressionType.GZIP | CompressionType.GUNZIP => stream.via(ZPipeline.gunzip(64 * 1024))
      case NONE => stream
    }

  def readBytes[T>:Null :SchemaFor :Decoder :Encoder, Config <: S3Config](file: S3ObjectSummary):
  ZIO[S3 with Config, Exception, ZStream[Any, Nothing, T]] =
    for {
      config <- ZIO.service[S3Config]
      byteChunks <- decompressStream(config.compressionType, zio.s3.getObject(file.bucketName, file.key)).runCollect
    } yield {
      import com.libertexgroup.ape.utils.AvroUtils.implicits._
      ZStream.fromIterable(byteChunks.decode[T]())
    }

  def readParquet[T >:Null: SchemaFor :Encoder :Decoder, Config <: S3Config](file: S3ObjectSummary):
  ZIO[Config, Throwable, ZStream[Any, Throwable, T]] = for {
    config <- ZIO.service[S3Config]
    stream = readParquetWithType[T](config, file)
  } yield stream

  def readParquetGenericRecords[Config <: S3Config](file: S3ObjectSummary):
  ZIO[Config with S3, Throwable, ZStream[Any, Throwable, GenericRecord]] =
    for {
      config <- ZIO.service[S3Config]
      stream = readParquetGenericRecord(config, file)
    } yield stream

  def readFileStream[T](stream: ZStream[S3, Throwable, T]): ZIO[S3, Throwable, ZStream[Any, IOException, T]]
  = for {
    queue <- Queue.unbounded[T]
    rand <- ZIO.random
    queueName <- rand.nextPrintableChar
    _ <- printLine(s"Reading stream with back pressure (using queue ${queueName})")
    count <- stream.tap(msg => queue.offer(msg)).runCount
  } yield ZStream
    .range(0, count.toInt)
    .mapZIO(_ => for {
      t <- queue.take
      _ <- printLine(s"Reading stream with back pressure (using queue ${queueName})")
    } yield t)
    .ensuring(queue.shutdown <* printLine(s"Shutting down queue ${queueName}").catchAll(_ => ZIO.unit))

  def readWithBackPressure[T, Config <: S3Config](inputStream:ZStream[Config with S3, Throwable, S3FileWithContent[T]]):
  ZStream[Config with S3, Throwable, (S3ObjectSummary, ZStream[Any, IOException, T])] = for {
      streams <- inputStream.mapZIO {
        case (file, stream) => readFileStream(stream).map(x => (file, x))
      }
    } yield streams
}
