package ape.s3.pipes.fromS3Files

import ape.s3.configs.{CSVConfig, S3Config}
import ape.s3.readers.{S3FileWithContent, readBytes, readPlainText}
import ape.utils.Utils.reLayer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import io.circe.jawn
import purecsv.safe.CSVReader
import purecsv.safe.converter.RawFieldsConverter
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

object S3FilePipe {
  def avroPipe[ZE, T: SchemaFor : Decoder : Encoder, S3Cfg <: S3Config : Tag](i: ZStream[ZE, Throwable, S3ObjectSummary]):
  ZIO[S3Cfg, Nothing, ZStream[ZE, Throwable, S3FileWithContent[T]]] = for {
    cfg <- ZIO.service[S3Cfg]
    s3 <- reLayer[S3Cfg]
  } yield i
    .mapZIO(file => for {
      content <- readBytes[T, S3Cfg](file).provideLayer(s3 ++ cfg.liveS3)
    } yield (file, content))

  def jsonLinesPipe[ZE, T, S3Cfg <: S3Config : Tag](i: ZStream[ZE, Throwable, S3ObjectSummary], decode: String => T):
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

  def jsonLinesCircePipe[ZE, T: io.circe.Decoder, S3Cfg <: S3Config : Tag](i: ZStream[ZE, Throwable, S3ObjectSummary]):
  ZIO[S3Cfg, Nothing, ZStream[ZE, Throwable, S3FileWithContent[T]]] = for {
    cfg <- ZIO.service[S3Cfg]
    s3 <- reLayer[S3Cfg]
  } yield i.map(file => {
    val content = readPlainText(cfg.compressionType, file)
      .provideSomeLayer(s3 ++ cfg.liveS3)
      .map(l => jawn.decode[T](l).toOption).filter(_.isDefined).map(_.get)
    (file, content)
  })

  def textPipe[ZE, S3Cfg <: S3Config : Tag](i: ZStream[ZE, Throwable, S3ObjectSummary]):
  ZIO[S3Cfg, Nothing, ZStream[ZE, Throwable, S3FileWithContent[String]]] = for {
    cfg <- ZIO.service[S3Cfg]
    s3 <- reLayer[S3Cfg]
  } yield i.map(file =>
    (file, readPlainText(cfg.compressionType, file).provideSomeLayer(s3 ++ cfg.liveS3))
  )

  def csvPipe[ZE, S3Cfg <: S3Config : Tag, T: ClassTag, CsvCfg <: CSVConfig : Tag]
  (i: ZStream[ZE, Throwable, S3ObjectSummary])(implicit rfcImp: RawFieldsConverter[T]):
    ZIO[S3Cfg with CsvCfg, Nothing, ZStream[ZE, Throwable, S3FileWithContent[T]]] = for {
      csvCfg <- ZIO.service[CsvCfg]
      cfg <- ZIO.service[S3Cfg]
      s3 <- reLayer[S3Cfg]
    } yield i.map(file => {
      val stream = readPlainText(cfg.compressionType, file)
        .map(l => CSVReader[T].readCSVFromString(l, csvCfg.delimiter, csvCfg.trimming, csvCfg.headers, csvCfg.headerMapping))
        .map(a => a.map(_.toOption).filter(_.isDefined).map(_.get))
        .flatMap(ZStream.fromIterable(_))
      (file, stream.provideSomeLayer(s3 ++ cfg.liveS3))
    })
}
