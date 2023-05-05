package com.libertexgroup.ape.writers.s3.fromS3Files.zioBacked

import com.libertexgroup.ape.readers.s3.S3FileWithContent
import com.libertexgroup.ape.writers.s3.fromS3Files.{S3ContentPipe, S3FilePipe, S3WithBackPressure}
import com.libertexgroup.configs.S3Config
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 *
 */
protected[s3] class TextPipe[
  Config <: S3Config :Tag
] extends S3ContentPipe[Config, Any, String] {
  override protected[this] def pipe(i: ZStream[Any, Throwable, S3ObjectSummary]):
    ZIO[Config, Throwable, ZStream[Any, Throwable, S3FileWithContent[String]]] = for {
    s <- S3FilePipe.textPipe(i)
  } yield s.map(S3WithBackPressure.zio[String].backPressure(_))
}
