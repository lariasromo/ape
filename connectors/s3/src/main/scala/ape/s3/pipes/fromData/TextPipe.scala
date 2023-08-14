package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.S3Utils.{uploadCompressedGroupedStream, uploadStream}
import zio.s3.{MultipartUploadOptions, S3, S3ObjectListing, S3ObjectSummary, multipartUpload}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, Tag, ZIO}

protected[s3] class TextPipe[E,
  Config <: S3Config :Tag
]
  extends S3Pipe[E with Config, E, String, S3ObjectSummary] {

  override protected[this] def pipe(i: ZStream[E, Throwable, String]):
    ZIO[E with Config, Throwable, ZStream[E, Throwable, S3ObjectSummary]] =
    for {
      files <- uploadCompressedGroupedStream[E, Config]{
        i
          .map(s => s"$s\n".getBytes)
          .flatMap(r => ZStream.fromIterable(r))
      }
    } yield ZStream.fromChunk(files)
}