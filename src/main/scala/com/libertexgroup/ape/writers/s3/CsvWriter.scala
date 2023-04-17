package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import purecsv.unsafe.converter.StringConverterUtils
import zio.{Tag, ZIO}
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[s3] class CsvWriter[ZE, T: ClassTag,Config <: S3Config :Tag]
(
  sep: String = ",",
  order:Option[Seq[String]]=None
) extends S3Writer[ZE with S3 with Config, ZE, T, T] {
  def getTMap(cc: T): Map[String, Any] =
    cc.getClass.getDeclaredFields.foldLeft(Map.empty[String, Any]) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc).toString)
    }

  def a(stream: ZStream[ZE, Throwable, T]):
  ZIO[ZE with S3 with Config, Throwable, ZStream[ZE, Throwable, T]] =
    for {
      config <- ZIO.service[Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      bytesStream = stream
        .map(a => {
          val m = getTMap(a)
          order
            .map(_.map(k => m.getOrElse(k, "")))
            .getOrElse(m.values)
            .map(v => StringConverterUtils.quoteTextIfNecessary(v.toString))
            .mkString(sep)
        } + "\n")
        .map(_.getBytes)
        .flatMap(bytes => ZStream.fromIterable(bytes))
      fileName <- zio.Random.nextUUID
      _ <- multipartUpload(
        bucket,
        s"${location}/${fileName}.csv",
        bytesStream,
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield stream

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]):
    ZIO[ZE with S3 with Config, Throwable, ZStream[ZE, Throwable, T]] = a(i)
}