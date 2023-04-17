package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import io.circe.{Decoder, jawn}
import zio.{Tag, ZIO}
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class JsonLinesCirceReader[T: Decoder :ClassTag, Config <: S3Config :Tag]
  extends S3Reader[Config, S3 with Config, S3FileWithContent[T], Config] {
  def mapContent(x: ZStream[S3, Throwable, String]): ZStream[S3, Throwable, T] =
    x.map(l => jawn.decode[T](l).toOption).filter(_.isDefined).map(_.get)

  override protected[this] def read: ZIO[S3FileReaderService[Config] with Config, Throwable,
    ZStream[S3 with Config, Throwable, (S3ObjectSummary, ZStream[S3, Throwable, T])]] =
    for {
      config <- ZIO.service[Config]
      s3FilesQueue <- fileStream
      stream = s3FilesQueue.map(file => (file, mapContent(readPlainText(config.compressionType, file))))
    } yield stream
}
