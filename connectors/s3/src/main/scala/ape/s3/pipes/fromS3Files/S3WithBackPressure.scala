package ape.s3.pipes.fromS3Files

import ape.reader.Reader
import ape.redis.configs.RedisConfig
import ape.s3.readers.S3FileWithContent
import ape.utils.Utils.reLayer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

object S3WithBackPressure {
  class Redis[Config <: RedisConfig : Tag] {
    def backPressure[E, ZE, T >: Null : SchemaFor : Encoder : Decoder : ClassTag : Tag]:
    S3FileWithContent[T] => (S3ObjectSummary, ZStream[ZE with E with Config, Throwable, T]) = {
      case (f, s) => (f, {
        Reader.UnitReader[E, ZE, T](s) --> ape.redis.Pipes.pipes[Config].backPressure.finite[E, ZE].typed[T]
      })
    }

    def backPressureZ[T: SchemaFor : Encoder : Decoder : ClassTag : Tag](s: S3FileWithContent[T]):
    ZIO[Config, Nothing, S3FileWithContent[T]] =
      for {
        rcL <- reLayer[Config]
        pipe = Reader.UnitReader[Any, Any, T](s._2) --> ape.redis.Pipes.pipes[Config].backPressure.finite.typed[T]
      } yield (s._1, pipe.provideSomeLayer(rcL))
  }

  def redis[RC <: RedisConfig : Tag] = new Redis[RC]

  class ZIOBP[T: ClassTag] {
    def backPressure[E, ZE]: S3FileWithContent[T] => (S3ObjectSummary, ZStream[ZE with E, Throwable, T]) = {
      case (f, s) => (f, {
        Reader.UnitReader[E, ZE, T](s) --> ape.misc.Pipes.pipes.backPressure[ZE].finite[T]
      })
    }
  }

  def zio[T: ClassTag] = new ZIOBP[T]
}
