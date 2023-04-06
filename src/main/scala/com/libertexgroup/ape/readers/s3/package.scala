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

package object s3 {
  type S3FileWithContent[T, AWSS3 <: S3] = (S3ObjectSummary, ZStream[AWSS3, Throwable, T])

  def readPlainText[AWSS3 <: S3](compressionType: CompressionType, file: S3ObjectSummary): ZStream[AWSS3, Exception, String] =
  {
    val stream = zio.s3.getObject(file.bucketName, file.key)
    (compressionType match {
      case CompressionType.GZIP | CompressionType.GUNZIP => stream.via(ZPipeline.gunzip(64 * 1024))
      case NONE => stream
    }).via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
  }

  def decompressStream[AWSS3 <: S3](compressionType: CompressionType,
                       stream: ZStream[AWSS3, S3Exception, Byte]): ZStream[AWSS3, Exception, Byte] =
    compressionType match {
      case CompressionType.GZIP | CompressionType.GUNZIP => stream.via(ZPipeline.gunzip(64 * 1024))
      case NONE => stream
    }

  def readBytes[T>:Null :SchemaFor :Decoder :Encoder, AWSS3 <: S3, Config <: S3Config](file: S3ObjectSummary):
  ZIO[AWSS3 with Config, Exception, ZStream[Any, Nothing, T]] =
    for {
      config <- ZIO.service[S3Config]
      byteChunks <- decompressStream[AWSS3](config.compressionType, zio.s3.getObject(file.bucketName, file.key)).runCollect
    } yield {
      import com.libertexgroup.ape.utils.AvroUtils.implicits._
      ZStream.fromIterable(byteChunks.decode[T]())
    }

  def readParquet[T >:Null: SchemaFor :Encoder :Decoder, Config <: S3Config](file: S3ObjectSummary):
  ZIO[Config, Throwable, ZStream[Any, Throwable, T]] = for {
    config <- ZIO.service[S3Config]
    stream = readParquetWithType[T](config, file)
  } yield stream

  def readParquetGenericRecords[AWSS3 <: S3, Config <: S3Config](file: S3ObjectSummary):
  ZIO[Config with AWSS3, Throwable, ZStream[Any, Throwable, GenericRecord]] =
    for {
      config <- ZIO.service[S3Config]
      stream = readParquetGenericRecord(config, file)
    } yield stream

  def readFileStream[T, AWSS3 <: S3](stream: ZStream[AWSS3, Throwable, T]):
  ZIO[AWSS3, Throwable, ZStream[Any, Nothing, T]] = for {
    queue <- Queue.unbounded[T]
    rand <- ZIO.random
    queueName <- rand.nextPrintableChar
    _ <- printLine(s"Reading stream with back pressure (using queue ${queueName})")
    count <- stream.tap(msg => queue.offer(msg)).runCount
  } yield ZStream
    .range(0, count.toInt)
    .mapZIO(_ => queue.take)
    .ensuring(queue.shutdown <* printLine(s"Shutting down queue ${queueName}").catchAll(_ => ZIO.unit))

  def readWithBackPressure[T, AWSS3 <: S3, Config <: S3Config]
    (inputStream:ZStream[Config with AWSS3, Throwable, S3FileWithContent[T, AWSS3]]):
      ZStream[Config with AWSS3, Throwable, S3FileWithContent[T, AWSS3]] = for {
      streams <- inputStream.mapZIO {
        case (file, stream) => readFileStream(stream).map(x => (file, x))
      }
    } yield streams
}
