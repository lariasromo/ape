package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.S3Utils.uploadStream
import ape.utils.AvroUtils.implicits._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.{ListObjectOptions, MultipartUploadOptions, S3, S3ObjectListing, UploadOptions, listObjects, multipartUpload}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class AvroPipe[E,
  T >:Null :SchemaFor :Decoder :Encoder : ClassTag,
  Config <: S3Config :Tag
] extends S3Pipe[E with S3 with Config, E, T, S3ObjectListing] {

  override protected[this] def pipe(i: ZStream[E, Throwable, T]):
    ZIO[S3 with E with Config, Throwable, ZStream[E, Throwable, S3ObjectListing]] = for {
    config <- ZIO.service[Config]
    randomUUID <- zio.Random.nextUUID
    fileName =
      config.filePrefix.getOrElse("") +
        config.fileName.getOrElse(randomUUID) +
        config.fileSuffix.getOrElse("") + ".avro" + {if(config.compressionType.equals(CompressionType.GZIP)) ".gz"}
    bytesStream = i.map(r => { r.encode().orNull}).flatMap(r => ZStream.fromIterable(r))
    compressedStream = if(config.compressionType.equals(CompressionType.GZIP)) bytesStream.via(ZPipeline.gzip())
                        else bytesStream
    files <- config.chunkSizeMb match {
      case Some(size) =>
        compressedStream
        .grouped(size)
        .map(chk => ZStream.fromChunk(chk))
        .mapZIO(stream => uploadStream[E, Config](fileName, stream))
        .runCollect
      case None =>
        uploadStream[E, Config](fileName, compressedStream).flatMap(c => ZIO.succeed(Chunk(c)))
    }
  } yield ZStream.fromChunk(files)
}