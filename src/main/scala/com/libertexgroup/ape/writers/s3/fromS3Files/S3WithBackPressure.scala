package com.libertexgroup.ape.writers.s3.fromS3Files

import com.libertexgroup.ape.readers.s3.S3FileWithContent
import com.libertexgroup.ape.utils.Utils.reLayer
import com.libertexgroup.ape.{Ape, Reader, Writer}
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.{Tag, ZIO}
import zio.s3.S3ObjectSummary
import zio.stream.ZStream

import scala.reflect.ClassTag

object S3WithBackPressure {
  class Redis[Config <: RedisConfig :Tag] {
    def backPressure[E, ZE, T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]:
      S3FileWithContent[T] => (S3ObjectSummary, ZStream[ZE with E with Config, Throwable, T]) = {
        case (f, s) => (f, {
          Reader.unitReader[E, ZE, T](s) --> Ape.writers.redis[Config].backPressure.finite[ZE, T]
        })
    }
    def backPressureZ[T:SchemaFor :Encoder :Decoder :ClassTag :Tag](s: S3FileWithContent[T]):
      ZIO[Config, Nothing, S3FileWithContent[T]] =
      for {
        rcL <- reLayer[Config]
        pipe = Reader.unitReader[Any, Any, T](s._2) --> Ape.writers.redis[Config].backPressure.finite[Any, T]
      } yield (s._1, pipe.provideSomeLayer(rcL))
  }
  def redis[RC <: RedisConfig :Tag] = new Redis[RC]

  class ZIOBP[T :ClassTag]{
    def backPressure[E, ZE]: S3FileWithContent[T] => (S3ObjectSummary, ZStream[ZE with E, Throwable, T]) = {
      case (f, s) => (f, {
        Reader.unitReader[E, ZE, T](s) --> Ape.writers.misc.backPressureFinite[ZE, T]
      })
    }
  }
  def zio[T :ClassTag] = new ZIOBP[T]
}
