package com.libertexgroup.ape.writers.redis

import com.libertexgroup.ape.{Ape, Reader, Writer}
import com.libertexgroup.configs.RedisConfig
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Tag, ZIO}

// Use with caution as this writer will produce an infinite stream, even if your input stream is finite.
// If working with a finite stream use BackPressureFiniteWriter
class BackPressureInfiniteStringWriter[ZE, Config<:RedisConfig :Tag]
  extends Writer[ZE with Config, ZE, String, String] {
  def readWithBackPressureRedis(stream: ZStream[ZE, Throwable, String]): ZIO[ZE with Config, Throwable, ZStream[ZE, Throwable, String]] =
    for {
      rand <- ZIO.random
      queueName <- rand.nextString(10)
      _ <- printLine(s"Reading stream with back pressure (using Redis queue ${queueName})")
      _ <- {
        val r: Reader[Any, ZE, String] = new Reader.UnitReader[Any, ZE, String](stream)
        val w: Writer[Config, ZE, String, String] = Ape.writers.redis[Config].generalPurpose.default[ZE, String](queueName)
        r --> w
      }.runDrain.fork
      s <- Ape.readers.redis[Config].avro[Any, String](queueName).apply
    } yield s

  override protected[this] def pipe(i: ZStream[ZE, Throwable, String]): ZIO[ZE with Config, Throwable, ZStream[ZE, Throwable, String]] =
    readWithBackPressureRedis(i)
}
