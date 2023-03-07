package com.libertexgroup.ape.readers

import com.libertexgroup.ape.utils.ParquetUtils.{readParquetGenericRecord, readParquetWithType}
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import com.libertexgroup.models.CompressionType._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.avro.generic.GenericRecord
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.Console.printLine
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary}
import zio.stream.{Take, ZPipeline, ZSink, ZStream}
import zio.{Chunk, Queue, ZIO}

import scala.util.Try

package object s3 {
  def listFiles(bucket: String, location: String): ZIO[S3, S3Exception, Chunk[S3ObjectSummary]] =
    zio.s3.listObjects(bucket, ListObjectOptions.from(location, 100)).map(_.objectSummaries)

  def readPlainText(bucket: String, location: String): ZIO[S3 with S3Config, Throwable, ZStream[S3, Throwable, String]] =
    for {
      config <- ZIO.service[S3Config]
      chunk <- listFiles(bucket, location)
      lines = chunk
        .map(file => {
          zio.s3.getObject(file.bucketName, file.key)
        })
        .map(stream => {
          (config.compressionType match {
            case CompressionType.GZIP | CompressionType.GUNZIP => stream.via(ZPipeline.gunzip(64 * 1024))
            case NONE => stream
          }).via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
        })
        .fold(ZStream.empty)(_ ++ _)
      newStream <- if(config.enableBackPressure) readWithBackPressure(lines) else ZIO.succeed(lines)
    } yield newStream

  def decompressStream(compressionType: CompressionType,
                       stream: ZStream[S3, S3Exception, Byte]): ZStream[S3, Exception, Byte] =
    compressionType match {
      case CompressionType.GZIP | CompressionType.GUNZIP => stream.via(ZPipeline.gunzip(64 * 1024))
      case NONE => stream
    }

  def readBytes[T>:Null :SchemaFor :Decoder :Encoder](bucket: String, location: String): ZIO[S3 with S3Config, Exception, ZStream[Any, Nothing, T]] =
    for {
      config <- ZIO.service[S3Config]
      chunk <- listFiles(bucket, location)
      byteChunks <- chunk
        .map(file => {
          zio.s3.getObject(file.bucketName, file.key)
        })
        .map(decompressStream(config.compressionType, _))
        .fold(ZStream.empty)(_ ++ _)
        .runCollect
    } yield {
      import com.libertexgroup.ape.utils.AvroUtils.implicits._
      ZStream.fromIterable(byteChunks.decode[T]())
    }

  def readParquet[T >:Null: SchemaFor :Encoder :Decoder](location:String):
  ZIO[S3 with S3Config, Throwable, ZStream[Any, Throwable, T]] = for {
    config <- ZIO.service[S3Config]
    bucket <- config.taskS3Bucket
    chunk <- listFiles(bucket, location)
    stream = ZStream.fromChunk(chunk).flatMap(file => readParquetWithType[T](config, file))
    newStream <- if(config.enableBackPressure) readWithBackPressure(stream) else ZIO.succeed(stream)
  } yield newStream

  def readParquetGenericRecords(location:String): ZIO[S3 with S3Config, Throwable, ZStream[Any, Throwable, GenericRecord]] = for {
    config <- ZIO.service[S3Config]
    bucket <- config.taskS3Bucket
    chunk <- listFiles(bucket, location)
    stream = ZStream.fromChunk(chunk).flatMap(file => readParquetGenericRecord(config, file))
    newStream <- if(config.enableBackPressure) readWithBackPressure(stream) else ZIO.succeed(stream)
  } yield newStream

  def readWithBackPressure[E, T](stream:ZStream[E, Throwable, T]): ZIO[E, Throwable, ZStream[Any, Throwable, T]]
  =
    for {
      queue <- Queue.unbounded[T]
      count <- stream.tap(msg => queue.offer(msg)).runCount
      _ <- printLine(s"Offered ${count} messages to queue")
    } yield ZStream.range(0, count.toInt)
      .tap(m => printLine(s"Taking message: $m from queue"))
      .mapZIO(_ => queue.take)
      .ensuring(queue.shutdown)

}
