package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.utils.ParquetUtils
import com.libertexgroup.configs.S3Config
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{MultipartUploadOptions, S3, multipartUpload}
import zio.stream.ZStream
import zio.{Duration, ZIO}

class ParquetWriter[E, T >:Null: SchemaFor :Encoder :Decoder](chunkSize: Int,
                                                              duration: Duration) extends S3Writer[E, S3 with E with S3Config, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[S3 with E with S3Config, Throwable, Unit] =
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
    } yield ()

}