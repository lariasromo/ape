package com.libertexgroup.ape.writers.s3.fromData

import com.libertexgroup.ape.utils.ParquetUtils
import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream
import zio.{Duration, Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class ParquetWriter[E,
  T >:Null: SchemaFor :Encoder :Decoder : ClassTag,
  Config <: S3Config :Tag](chunkSize: Int, duration: Duration)
  extends S3Writer[E with S3 with Config, E, T, T] {
  def a(stream: ZStream[E, Throwable, T]): ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, T]] =
    for {
      config <- ZIO.service[Config]
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

  override protected[this] def pipe(i: ZStream[E, Throwable, T]):
    ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, T]] = a(i)
}