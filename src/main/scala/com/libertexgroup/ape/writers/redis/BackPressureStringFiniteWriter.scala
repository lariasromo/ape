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
class BackPressureStringFiniteWriter[ZE, Config<:RedisConfig :Tag]
  extends Writer[ZE with Config, ZE, String, String] {
  def readWithBackPressureRedis(stream: ZStream[ZE, Throwable, String]):
    ZIO[ZE with Config, Throwable, ZStream[ZE, Throwable, String]] =
    for {
      queueName <- ZIO.succeed(Random.alphanumeric.filter(_.isLetter).take(10).mkString)
      _ <- printLine(s"Reading stream with back pressure (using Redis queue ${queueName})")
      count <- {
        val r: Reader[Any, ZE, String] = new Reader.UnitReader[Any, ZE, String](stream)
        val w: Writer[Config, ZE, String, String] = Ape.writers.redis[Config].generalPurpose.string[ZE](queueName)
        r --> w
      }.runCount
      s <- Ape.readers.redis[Config].string[Any](queueName, count.toInt).apply
    } yield s

  override protected[this] def pipe(i: ZStream[ZE, Throwable, String]):
    ZIO[ZE with Config, Throwable, ZStream[ZE, Throwable, String]] = readWithBackPressureRedis(i)
}
