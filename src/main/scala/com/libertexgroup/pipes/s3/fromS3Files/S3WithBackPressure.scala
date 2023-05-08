package com.libertexgroup.pipes.s3.fromS3Files

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.RedisConfig
import com.libertexgroup.readers.s3.S3FileWithContent
import com.libertexgroup.utils.Utils.reLayer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

object S3WithBackPressure {
  class Redis[Config <: RedisConfig :Tag] {
    def backPressure[E, ZE, T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]:
      S3FileWithContent[T] => (S3ObjectSummary, ZStream[ZE with E with Config, Throwable, T]) = {
        case (f, s) => (f, {
          Reader.UnitReader[E, ZE, T](s) --> Ape.pipes.redis[Config].backPressure.finite[E, ZE].typed[T]
        })
    }
    def backPressureZ[T:SchemaFor :Encoder :Decoder :ClassTag :Tag](s: S3FileWithContent[T]):
      ZIO[Config, Nothing, S3FileWithContent[T]] =
      for {
        rcL <- reLayer[Config]
        pipe = Reader.UnitReader[Any, Any, T](s._2) --> Ape.pipes.redis[Config].backPressure.finite.typed[T]
      } yield (s._1, pipe.provideSomeLayer(rcL))
  }
  def redis[RC <: RedisConfig :Tag] = new Redis[RC]

  class ZIOBP[T :ClassTag]{
    def backPressure[E, ZE]: S3FileWithContent[T] => (S3ObjectSummary, ZStream[ZE with E, Throwable, T]) = {
      case (f, s) => (f, {
        Reader.UnitReader[E, ZE, T](s) --> Ape.pipes.misc.backPressure[ZE].finite[T]
      })
    }
  }
  def zio[T :ClassTag] = new ZIOBP[T]
}
