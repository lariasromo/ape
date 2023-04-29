package com.libertexgroup.ape.writers.s3.fromS3Files

import com.libertexgroup.ape.readers.s3.{S3FileWithContent, readPlainText}
import com.libertexgroup.ape.utils.Utils.reLayer
import com.libertexgroup.configs.S3Config
import io.circe.{Decoder, jawn}
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */

class JsonLinesCircePipe[ZE, T: Decoder :ClassTag, Config <: S3Config :Tag]
  extends S3ContentPipe[Config, ZE, T ] {
  def mapContent(x: ZStream[S3, Throwable, String]): ZStream[S3, Throwable, T] =
    x.map(l => jawn.decode[T](l).toOption).filter(_.isDefined).map(_.get)

  override protected[this] def pipe(i: ZStream[ZE, Throwable, S3ObjectSummary]):
    ZIO[Config, Throwable, ZStream[ZE, Throwable, S3FileWithContent[T]]] = for {
    cl <- reLayer[Config]
    config <- ZIO.service[Config]
  } yield i.map(file =>
      (
        file,
        mapContent(
          readPlainText(config.compressionType, file).provideSomeLayer(cl ++ config.liveS3)
        )
      )
  )
}