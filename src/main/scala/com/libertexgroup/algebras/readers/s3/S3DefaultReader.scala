package com.libertexgroup.algebras.readers.s3

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.EncodingType._
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.hadoop.util.HadoopInputFile
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.s3.{ListObjectOptions, S3, S3ObjectSummary}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, ZIO}


/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
class S3DefaultReader extends Reader[S3 with S3Config, S3, GenericRecord] {

  override def apply: ZIO[S3 with S3Config, Throwable, ZStream[S3, Throwable, GenericRecord]]
  =
    for {
      config <- ZIO.service[S3Config]
      bucket <- config.taskS3Bucket
      location <- config.taskLocation
      stream <- decodeS3Files(bucket, location)
    } yield stream

  def readFiles(bucket: String, location: String): ZIO[S3, S3Exception, Chunk[S3ObjectSummary]] =
    zio.s3.listObjects(bucket, ListObjectOptions.from(location, 100)).map(_.objectSummaries)

  def decodeS3Files(bucket: String, location: String): ZIO[S3 with S3Config, Throwable, ZStream[S3, Throwable, GenericRecord]] =
    for {
      config <- ZIO.service[S3Config]
      lines <- config.encodingType match {
          case AVRO => throw new Exception("Reading AVRO from S3 hasn't been implemented")
          case PARQUET => readParquet(bucket, location)
          case GZIP | GUNZIP | PLAINTEXT => readText(bucket, location)
        }
    } yield lines

  def readText(bucket: String, location: String): ZIO[S3 with S3Config, S3Exception, ZStream[S3, Exception, GenericData.Record]] =
    for {
      config <- ZIO.service[S3Config]
      chunk <- readFiles(bucket, location)
      lines = chunk
        .map(file => {
          zio.s3.getObject(file.bucketName, file.key)
        })
        .map(stream => config.encodingType match {
          case PLAINTEXT =>
            stream.via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          case GZIP | GUNZIP =>
            stream.via(ZPipeline.gunzip(64 * 1024)).via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
        })
        .fold(ZStream.empty)(_ ++ _)
        .map(l => {
          new GenericRecordBuilder(
            SchemaBuilder
              .record("S3File")
              .fields()
              .name("value").`type`().stringType().stringDefault("")
              .endRecord()
          ).set("value", l).build()
        })
    } yield lines

  def readParquet(bucket: String, location: String): ZIO[S3 with S3Config, S3Exception, ZStream[Any, Throwable, GenericRecord]] = for {
    config <- ZIO.service[S3Config]
    chunk <- readFiles(bucket, location)
    stream = ZStream.fromChunk(chunk)
      .flatMap(file => {
        val path = new Path(s"s3a://${file.bucketName}/${file.key}")
        val conf = new Configuration()
        conf.set("fs.s3a.access.key", config.awsAccessKey)
        conf.set("fs.s3a.secret.key", config.awsSecretKey)
        conf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
        conf.setBoolean("fs.s3a.path.style.access", true)
        conf.setBoolean(org.apache.parquet.avro.AvroReadSupport.READ_INT96_AS_FIXED, true)

        ZStream.acquireReleaseWith(
            ZIO.succeed(AvroParquetReader.builder[GenericRecord](
              HadoopInputFile.fromPath(path, conf)
            ).build)
          )(x => ZIO.succeed(x.close()))
          .flatMap { is =>
            ZStream.succeed(is.read())
          }
      })
  } yield stream
}
