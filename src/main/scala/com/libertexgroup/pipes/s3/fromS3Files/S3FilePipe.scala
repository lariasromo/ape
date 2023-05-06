package com.libertexgroup.pipes.s3.fromS3Files

import com.libertexgroup.configs.S3Config
import com.libertexgroup.readers.s3.{S3FileWithContent, readBytes, readPlainText}
import com.libertexgroup.utils.Utils.reLayer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe.jawn
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

object S3FilePipe {
  def avroPipe[ZE, T:SchemaFor :Decoder :Encoder, S3Cfg <: S3Config :Tag](i: ZStream[ZE, Throwable, S3ObjectSummary]):
    ZIO[S3Cfg, Nothing, ZStream[ZE, Throwable, S3FileWithContent[T]]] = for {
      cfg <- ZIO.service[S3Cfg]
      s3 <- reLayer[S3Cfg]
    } yield i
    .mapZIO(file => for {
      content <- readBytes[T, S3Cfg](file).provideLayer(s3 ++ cfg.liveS3)
    } yield (file, content))

  def jsonLinesPipe[ZE, T, S3Cfg <: S3Config :Tag](i: ZStream[ZE, Throwable, S3ObjectSummary], decode: String => T):
    ZIO[S3Cfg, Nothing, ZStream[ZE, Throwable, S3FileWithContent[T]]] = for {
      cfg <- ZIO.service[S3Cfg]
      s3 <- reLayer[S3Cfg]
    } yield i.map(file =>
      (
        file,
        readPlainText(cfg.compressionType, file)
          .provideSomeLayer(s3 ++ cfg.liveS3)
          .map(decode)
      )
    )

  def jsonLinesCircePipe[ZE, T :io.circe.Decoder, S3Cfg <: S3Config :Tag](i: ZStream[ZE, Throwable, S3ObjectSummary]):
    ZIO[S3Cfg, Nothing, ZStream[ZE, Throwable, S3FileWithContent[T]]] = for {
      cfg <- ZIO.service[S3Cfg]
      s3 <- reLayer[S3Cfg]
    } yield i.map(file => {
      val content = readPlainText(cfg.compressionType, file)
        .provideSomeLayer(s3 ++ cfg.liveS3)
        .map(l => jawn.decode[T](l).toOption).filter(_.isDefined).map(_.get)
      (file, content)
    })

  def textPipe[ZE, S3Cfg <: S3Config :Tag](i: ZStream[ZE, Throwable, S3ObjectSummary]):
    ZIO[S3Cfg, Nothing, ZStream[ZE, Throwable, S3FileWithContent[String]]] = for {
      cfg <- ZIO.service[S3Cfg]
      s3 <- reLayer[S3Cfg]
    } yield i.map(file =>
      (file, readPlainText(cfg.compressionType, file).provideSomeLayer(s3 ++ cfg.liveS3))
    )
}
