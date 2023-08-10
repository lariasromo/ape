package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.S3Utils.uploadStream
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
      config <- ZIO.service[Config]
      randomUUID <- zio.Random.nextUUID
      fileName = config.filePrefix.getOrElse("") +
        config.fileName.getOrElse(randomUUID) + ".txt" +
        config.fileSuffix.getOrElse("") +
        {if(config.compressionType.equals(CompressionType.GZIP)) ".gz" else ""}
      bytesStream = i.map(s => s"$s\n".getBytes).flatMap(r => ZStream.fromIterable(r))
      compressedStream = if(config.compressionType.equals(CompressionType.GZIP)) bytesStream.via(ZPipeline.gzip())
      else bytesStream
      files <- config.chunkSizeMb match {
        case Some(size) =>
          compressedStream
            .grouped(size)
            .map(chk => ZStream.fromChunk(chk))
            .mapZIO(stream => uploadStream[E, Config](fileName, stream).provideSomeLayer[E with Config](config.liveS3))
            .runCollect
        case None =>
          uploadStream[E, Config](fileName, compressedStream)
            .flatMap(c => ZIO.succeed(Chunk(c)))
            .provideSomeLayer[E with Config](config.liveS3)
      }
    } yield ZStream.fromChunk(files)
}