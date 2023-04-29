package com.libertexgroup.ape.writers.s3.fromS3Files

import com.libertexgroup.ape.readers.s3.S3FileWithContent
import com.libertexgroup.ape.{Ape, Reader, Writer}
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag
import zio.s3.S3ObjectSummary
import zio.stream.ZStream

import scala.reflect.ClassTag

object S3WithBackPressure {
  def redis[RC <: RedisConfig :Tag] = new Redis[RC]
  class Redis[Config <: RedisConfig :Tag] {
    def backPressure[E, ZE, T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]:
      S3FileWithContent[T] => (S3ObjectSummary, ZStream[ZE with E with Config, Throwable, T]) = {
        case (f, s) => (f, {
          Reader.unitReader[E, ZE, T](s) --> Ape.writers.redis[Config].backPressure.finite[ZE, T]
        })
    }
  }

  def zio[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag] = new ZIOBP[T]
  class ZIOBP[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]{
    def backPressure[E, ZE]: S3FileWithContent[T] => (S3ObjectSummary, ZStream[ZE with E, Throwable, T]) = {
      case (f, s) => (f, {
        Reader.unitReader[E, ZE, T](s) --> Ape.writers.misc.backPressureFinite[ZE, T]
      })
    }
  }
}
