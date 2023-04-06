package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import zio.{Tag, ZIO}
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class JsonLinesReader[T :ClassTag, Config <: S3Config :Tag]
(implicit decode: String => T) extends S3Reader[Config, S3 with Config, S3FileWithContent[T], Config] {

  override def apply: ZIO[S3FileReaderService[Config] with Config, Throwable,
    ZStream[S3 with Config, Throwable, S3FileWithContent[T]]] =
    for {
      config <- ZIO.service[S3Config]
      s3FilesQueue <- fileStream
      decodedStream = s3FilesQueue.map(file => (file, readPlainText(config.compressionType, file).map(decode)))
      newStream = if(config.enableBackPressure) readWithBackPressure[T, Config](decodedStream) else decodedStream
    } yield newStream
}
