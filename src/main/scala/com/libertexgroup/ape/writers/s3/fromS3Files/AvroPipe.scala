package com.libertexgroup.ape.writers.s3.fromS3Files

import com.libertexgroup.ape.readers.s3.{S3FileWithContent, readBytes}
import com.libertexgroup.ape.utils.Utils.reLayer
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.BackPressureType
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

/**
 * The result of the apply method will return a ZStream[S3, Throwable, GenericRecord]
 * The GenericRecord interface allows to interact with parquet values
 * If the file is just a text file each line will be a string stored in an attribute named `value`
 */
protected[s3] class AvroPipe[ZE, T :SchemaFor :Decoder :Encoder :ClassTag, Config <: S3Config :Tag]
  extends S3ContentPipe[ Config, ZE, T ] {
  override protected[this] def pipe(i: ZStream[ZE, Throwable, S3ObjectSummary]):
    ZIO[Config, Throwable, ZStream[ZE, Throwable, S3FileWithContent[T]]] = for {
      cl <- reLayer[Config]
      config <- ZIO.service[Config]
    } yield i
      .mapZIO(file => for {
          content <- readBytes[T, Config](file).provideSomeLayer(cl ++ config.liveS3)
        } yield (file, content))
      .map(data => config.backPressure match {
        case BackPressureType.NONE => data
        case BackPressureType.ZIO => S3WithBackPressure.zio[T].backPressure(data)
        case BackPressureType.REDIS => throw new Exception("REDIS back pressure should be configured on client side")
       })
}
