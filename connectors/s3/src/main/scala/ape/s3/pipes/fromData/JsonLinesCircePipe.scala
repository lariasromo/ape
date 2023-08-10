package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.S3Utils
import ape.s3.utils.S3Utils.uploadStream
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import zio.s3.{MultipartUploadOptions, S3, S3ObjectListing, S3ObjectSummary, multipartUpload}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class JsonLinesCircePipe[E,
  T: Encoder : ClassTag,
  Config <: S3Config :Tag
] extends S3Pipe[E with Config, E, T, S3ObjectSummary] {
  override protected[this] def pipe(stream: ZStream[E, Throwable, T]):
    ZIO[E with Config, Throwable, ZStream[E, Throwable, S3ObjectSummary]] =
    for {
      config <- ZIO.service[Config]
      bytesStream = stream
        .map(s => s.asJson.noSpaces + "\n")
        .map(_.getBytes)
        .flatMap(bytes => ZStream.fromIterable(bytes))
      compressedStream = if(config.compressionType.equals(CompressionType.GZIP)) bytesStream.via(ZPipeline.gzip())
      else bytesStream
      files <- config.chunkSizeMb match {
        case Some(size) =>
          compressedStream
            .grouped(size)
            .map(chk => ZStream.fromChunk(chk))
            .mapZIO(stream => uploadStream[E, Config](stream).provideSomeLayer[E with Config](config.liveS3)).runCollect
        case None => uploadStream[E, Config](compressedStream)
          .flatMap(c => ZIO.succeed(Chunk(c)))
          .provideSomeLayer[E with Config](config.liveS3)
      }
    } yield ZStream.fromChunk(files)
}