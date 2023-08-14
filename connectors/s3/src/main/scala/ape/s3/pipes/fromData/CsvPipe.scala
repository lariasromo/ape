package ape.s3.pipes.fromData

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.S3Utils
import ape.s3.utils.S3Utils.{uploadCompressedGroupedStream, uploadStream}
import purecsv.unsafe.converter.StringConverterUtils
import zio.s3.{MultipartUploadOptions, S3, S3ObjectListing, S3ObjectSummary, multipartUpload}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, Tag, ZIO}

import scala.reflect.ClassTag

protected[s3] class CsvPipe[ZE, T: ClassTag,Config <: S3Config :Tag]
(
  sep: String = ",",
  order:Option[Seq[String]]=None
) extends S3Pipe[ZE with S3 with Config, ZE, T, S3ObjectSummary] {
  def getTMap(cc: T): Map[String, Any] =
    cc.getClass.getDeclaredFields.foldLeft(Map.empty[String, Any]) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc).toString)
    }

  override protected[this] def pipe(stream: ZStream[ZE, Throwable, T]):
    ZIO[ZE with S3 with Config, Throwable, ZStream[ZE, Throwable, S3ObjectSummary]] =
    for {
      files <- uploadCompressedGroupedStream{
        stream
          .map(a => {
            val m = getTMap(a)
            order
              .map(_.map(k => m.getOrElse(k, "")))
              .getOrElse(m.values)
              .map(v => StringConverterUtils.quoteTextIfNecessary(v.toString))
              .mkString(sep)
          } + "\n")
          .map(_.getBytes)
          .flatMap(bytes => ZStream.fromIterable(bytes))
      }
    } yield ZStream.fromChunk(files)
}