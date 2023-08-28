package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.S3Utils.{splitStream, uploadCompressedGroupedStream, uploadStream}
import ape.utils.AvroUtils.implicits._
import ape.utils.Utils.reLayer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{ListObjectOptions, MultipartUploadOptions, S3, S3ObjectListing, S3ObjectSummary, UploadOptions, listObjects, multipartUpload}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class AvroPipe[E,
  T >:Null :SchemaFor :Decoder :Encoder : ClassTag,
  Config <: S3Config :Tag
] extends S3Pipe[E with Config, E, T, S3ObjectSummary] {

  override protected[this] def pipe(i: ZStream[E, Throwable, T]):
    ZIO[E with Config, Throwable, ZStream[E, Throwable, S3ObjectSummary]] = for {
      cfgL <- reLayer[Config]
      splitted <- splitStream(i)
      files = splitted.mapZIO { s =>
        for {
          files <- uploadCompressedGroupedStream[E, Config]{
            s.map(_.encode().orNull)
              .flatMap(r => ZStream.fromIterable(r))
          }.provideSomeLayer[E](cfgL)
        } yield files
      }
    } yield files.flatMap(c => ZStream.fromChunk(c))
}