package ape.s3.pipes.fromS3Files.zioBacked

import ape.s3.configs.{CSVConfig, S3Config}
import ape.s3.pipes.fromS3Files.{S3ContentPipe, S3FilePipe, S3WithBackPressure}
import ape.s3.readers.S3FileWithContent
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 *
 */
protected[s3] class CsvPipe[
  Config <: S3Config :Tag,
  CsvCfg <: CSVConfig,
  T: ClassTag
] extends S3ContentPipe[Config, Any, T] {
  override protected[this] def pipe(i: ZStream[Any, Throwable, S3ObjectSummary]):
    ZIO[Config with CsvCfg, Throwable, ZStream[Any, Throwable, S3FileWithContent[T]]] = for {
    s <- S3FilePipe.csvPipe[Any, Config, T, CsvCfg](i)
  } yield s.map(S3WithBackPressure.zio[T].backPressure(_))
}
