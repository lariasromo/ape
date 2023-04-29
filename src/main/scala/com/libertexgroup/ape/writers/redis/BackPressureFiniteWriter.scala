package com.libertexgroup.ape.writers.redis

import com.libertexgroup.ape.{Ape, Reader, Writer}
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag
import scala.util.Random

// Use with caution as this writer will consume stream first then produce a new stream
// So the first stream needs to be finite, if reading from an infinite stream, use BackPressureInfiniteWriter
class BackPressureFiniteWriter[ZE, Config<:RedisConfig :Tag, T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]
  extends Writer[ZE with Config, ZE, T, T] {
  def readWithBackPressureRedis(stream: ZStream[ZE, Throwable, T]): ZIO[ZE with Config, Throwable, ZStream[ZE, Throwable, T]] =
    for {
      queueName <- ZIO.succeed(Random.alphanumeric.filter(_.isLetter).take(10).mkString)
      _ <- printLine(s"Reading stream with back pressure (using Redis queue ${queueName})")
      count <- {
        val r: Reader[Any, ZE, T] = new Reader.UnitReader[Any, ZE, T](stream)
        val w: Writer[Config, ZE, T, T] = Ape.writers.redis[Config].generalPurpose.default[ZE, T](queueName)
        r --> w
      }.runCount
      s <- Ape.readers.redis[Config].avro[Any, T](queueName, count.toInt).apply
    } yield s

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[ZE with Config, Throwable, ZStream[ZE, Throwable, T]] =
    readWithBackPressureRedis(i)
}
