package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.utils.ParquetUtils
import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream
import zio.{Duration, Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class ParquetWriter[E,
  T >:Null: SchemaFor :Encoder :Decoder : ClassTag,
  Config <: S3Config :Tag,
  AWSS3 <: S3 :Tag](chunkSize: Int, duration: Duration)
  extends S3Writer[E with AWSS3 with Config, E, T, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E with AWSS3 with Config, Throwable, ZStream[E, Throwable, T]] =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      randomString <- zio.Random.nextUUID
      _ <- multipartUpload(
        bucket,
        s"${location}/${randomString}.parquet",
        stream
          .groupedWithin(chunkSize, duration)
          .mapZIO(ParquetUtils.recordsToParquetBytes[T])
          .flatMap(r => ZStream.fromIterable(r)),
        MultipartUploadOptions.default
      )(config.parallelism)
        .catchAll(_ => ZIO.unit)
    } yield stream
}