package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import io.circe.{Decoder, jawn}
import zio.{Queue, ZIO}
import zio.s3.S3
import zio.stream.ZStream

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[readers] class JsonLinesReader[T :ClassTag](implicit decode: String => T)
  extends S3Reader[S3Config, S3 with S3Config, S3FileWithContent[T]] {

  override def apply: ZIO[S3FileReaderService with S3Config, Throwable,
    ZStream[S3 with S3Config, Throwable, S3FileWithContent[T]]] =
    for {
      config <- ZIO.service[S3Config]
      s3FilesQueue <- fileStream
      decodedStream = s3FilesQueue.map(file => (file, readPlainText(config.compressionType, file).map(decode)))
      newStream = if(config.enableBackPressure) readWithBackPressure(decodedStream) else decodedStream
    } yield newStream
}
