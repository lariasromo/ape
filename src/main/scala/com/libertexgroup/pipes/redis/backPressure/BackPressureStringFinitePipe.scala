package com.libertexgroup.pipes.redis.backPressure

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.RedisConfig
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.util.Random

// Use with caution as this writer will consume stream first then produce a new stream
// So the first stream needs to be finite, if reading from an infinite stream, use BackPressureInfiniteWriter
class BackPressureStringFinitePipe[ZE, Config<:RedisConfig :Tag]
  extends Pipe[ZE with Config, ZE, String, String] {
  def readWithBackPressureRedis(stream: ZStream[ZE, Throwable, String]):
    ZIO[ZE with Config, Throwable, ZStream[ZE, Throwable, String]] =
    for {
      queueName <- ZIO.succeed(Random.alphanumeric.filter(_.isLetter).take(10).mkString)
      _ <- printLine(s"Reading stream with back pressure (using Redis queue ${queueName})")
      count <- {
        val r: Reader[Any, ZE, String] = Reader.UnitReader[Any, ZE, String](stream)
        val w: Pipe[Config, ZE, String, String] = Ape.pipes.redis[Config].generalPurpose.string[ZE](queueName)
        r --> w
      }.runCount
      s <- Ape.readers.redis[Config].string[Any](queueName, count.toInt).apply
    } yield s

  override protected[this] def pipe(i: ZStream[ZE, Throwable, String]):
    ZIO[ZE with Config, Throwable, ZStream[ZE, Throwable, String]] = readWithBackPressureRedis(i)
}
