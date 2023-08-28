package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.S3Utils.{splitStream, uploadCompressedGroupedStream, uploadStream}
import ape.utils.Utils.reLayer
import zio.s3.{MultipartUploadOptions, S3, S3ObjectListing, S3ObjectSummary, multipartUpload}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class JsonLinesPipe[E,
  T: ClassTag,
  Config <: S3Config :Tag
](implicit enc: T => String)
 extends S3Pipe[E with Config, E, T, S3ObjectSummary] {
  override protected[this] def pipe(i: ZStream[E, Throwable, T]):
    ZIO[E with Config, Throwable, ZStream[E, Throwable, S3ObjectSummary]] =
    for {
      cfgL <- reLayer[Config]
      splitted <- splitStream(i)
      files = splitted.mapZIO { s =>
        for {
          files <- uploadCompressedGroupedStream[E, Config]{
            s
              .map(s => enc(s) + "\n")
              .map(_.getBytes)
              .flatMap(bytes => ZStream.fromIterable(bytes))
          }.provideSomeLayer[E](cfgL)
        } yield files
      }
    } yield files.flatMap(c => ZStream.fromChunk(c))
}