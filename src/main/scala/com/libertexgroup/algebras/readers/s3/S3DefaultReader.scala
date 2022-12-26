package com.libertexgroup.algebras.readers.s3

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.EncodingType._
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary}
import zio.stream.ZTransducer.gunzip
import zio.stream.{ZStream, ZTransducer}
import zio.{Chunk, Has, ZIO}

class S3DefaultReader extends Reader[S3 with Has[S3Config], S3, String] {

  override def apply: ZIO[S3 with Has[S3Config], Throwable, ZStream[S3, Exception, String]]
  =
    for {
      config <- ZIO.access[Has[S3Config]](_.get)
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      stream <- decodeS3Files(bucket, location)
    } yield stream

  def readFiles(bucket: String, location: String): ZIO[S3, S3Exception, Chunk[S3ObjectSummary]] =
    zio.s3.listObjects(bucket, ListObjectOptions.from(location, 100)).map(_.objectSummaries)

  def decodeS3Files(bucket: String, location: String): ZIO[S3 with Has[S3Config], Throwable, ZStream[S3, Exception, String]] =
    for {
      config <- ZIO.access[Has[S3Config]](_.get)
      chunk <- readFiles(bucket, location)
      lines <- ZIO {
        chunk
          .map(file => {
            zio.s3.getObject(file.bucketName, file.key)
          })
          .map(stream => config.encodingType match {
            case PARQUET => throw new Exception("Reading PARQUET from S3 hasn't been implemented")
            case AVRO => throw new Exception("Reading AVRO from S3 hasn't been implemented")
            case PLAINTEXT =>
              stream.transduce(ZTransducer.utf8Decode >>> ZTransducer.splitLines)
            case GZIP | GUNZIP =>
              stream.transduce(gunzip(64 * 1024)).transduce(ZTransducer.utf8Decode >>> ZTransducer.splitLines)
          })
          .fold(ZStream.empty)(_ ++ _)
      }
    } yield lines

}
