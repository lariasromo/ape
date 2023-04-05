package com.libertexgroup.ape.writers.s3

import com.libertexgroup.configs.S3Config
import zio.ZIO
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream

import scala.reflect.ClassTag
import purecsv.safe._
import purecsv.safe.converter.RawFieldsConverter
import purecsv.unsafe.RecordSplitter.defaultFieldSeparatorStr
import purecsv.unsafe.converter.Converter

protected[writers] class CsvWriter[ZE, T: ClassTag](sep: String = defaultFieldSeparatorStr)
 extends S3Writer[ZE with S3 with S3Config, ZE, T, T] {
implicit val c = new RawFieldsConverter
  override def apply(stream: ZStream[ZE, Throwable, T]): ZIO[ZE with S3 with S3Config, Throwable, ZStream[ZE,
    Throwable, T]] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      bytesStream = stream
        .map(s => new CSVRecord(s).toCSV() + "\n")
        .map(_.getBytes)
        .flatMap(bytes => ZStream.fromIterable(bytes))
      _ <- multipartUpload(
        bucket,
        location,
        bytesStream,
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield stream
}