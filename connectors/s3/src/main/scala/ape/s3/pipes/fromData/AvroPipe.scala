package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.utils.AvroUtils.implicits._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{MultipartUploadOptions, S3, UploadOptions, multipartUpload}
import zio.stream.{ZPipeline, ZStream}
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class AvroPipe[E,
  T >:Null :SchemaFor :Decoder :Encoder : ClassTag,
  Config <: S3Config :Tag
] extends S3Pipe[E with S3 with Config, E, T, T] {

  override protected[this] def pipe(i: ZStream[E, Throwable, T]):
    ZIO[E with S3 with Config, Throwable, ZStream[E, Throwable, T]] = for {
    config <- ZIO.service[Config]
    bucket <- config.taskS3Bucket
    location <- config.taskLocation
    randomUUID <- zio.Random.nextUUID
    fileName = config.filePrefix.getOrElse("") + config.fileName.getOrElse(randomUUID) + config.fileSuffix.getOrElse("")
    bytesStream = i.map(r => { r.encode().orNull}).flatMap(r => ZStream.fromIterable(r))
    compressedStream = if(config.compressionType.equals(CompressionType.GZIP)) bytesStream.via(ZPipeline.gzip())
                        else bytesStream
    _ <- multipartUpload(
      bucket,
      s"${location}/${fileName}",
      compressedStream,
      MultipartUploadOptions.fromUploadOptions(UploadOptions.fromContentType("application/zip"))
    )(config.parallelism)
      .catchAll(_ => ZIO.unit)
  } yield i
}