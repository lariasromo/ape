package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import zio.{Tag, ZIO}
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class TextReader[Config <: S3Config :Tag]
  extends S3Reader[Config, Config with S3, S3FileWithContent[String], Config] {

  override def apply: ZIO[S3FileReaderService[Config] with Config, Throwable,
    ZStream[Config with S3, Throwable, S3FileWithContent[String]]] =
    for {
      config <- ZIO.service[Config]
      s3FilesQueue <- fileStream
      stream = s3FilesQueue.map(file => (file, readPlainText(config.compressionType, file)))
      newStream = if(config.enableBackPressure) readWithBackPressure[String, Config](stream) else stream
    } yield newStream
}
