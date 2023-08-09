package ape.s3.pipes.fromS3Files.noneBacked

import ape.s3.configs.{CSVConfig, S3Config}
import ape.s3.pipes.fromS3Files.{S3ContentPipe, S3FilePipe}
import ape.s3.readers.S3FileWithContent
import purecsv.safe.converter.RawFieldsConverter
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class CsvPipe[
  Config <: S3Config :Tag,
  CsvCfg <: CSVConfig :Tag,
  T: ClassTag
](implicit rfcImp: RawFieldsConverter[T]) extends S3ContentPipe[Config with CsvCfg, Any, T] {
  override protected[this] def pipe(i: ZStream[Any, Throwable, S3ObjectSummary]):
    ZIO[Config with CsvCfg, Throwable, ZStream[Any, Throwable, S3FileWithContent[T]]] =
    S3FilePipe.csvPipe[Any, Config, T, CsvCfg](i)
}
