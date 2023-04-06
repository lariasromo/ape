package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import io.circe.{Decoder, jawn}
import zio.{Tag, ZIO}
import zio.s3.S3
import zio.stream.ZStream

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class JsonLinesCirceReader[T: Decoder :ClassTag, Config <: S3Config :Tag, AWSS3 <: S3 :Tag]
  extends S3Reader[Config, AWSS3 with Config, S3FileWithContent[T, AWSS3], AWSS3, Config] {
  def mapContent(x: ZStream[AWSS3, Throwable, String]): ZStream[AWSS3, Throwable, T] =
    x.map(l => jawn.decode[T](l).toOption).filter(_.isDefined).map(_.get)

  override def apply: ZIO[S3FileReaderService[Config, AWSS3] with Config, Throwable,
    ZStream[AWSS3 with Config, Throwable, S3FileWithContent[T, AWSS3]]] =
    for {
      config <- ZIO.service[S3Config]
      s3FilesQueue <- fileStream
      stream = s3FilesQueue.map(file => (file, mapContent(readPlainText(config.compressionType, file))))
    } yield stream
}
