package ape.redis.pipes.backPressure

import ape.pipe.Pipe
import ape.reader.Reader
import ape.redis.configs.RedisConfig
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.util.Random

// Use with caution as this writer will consume stream first then produce a new stream
// So the first stream needs to be finite, if reading from an infinite stream, use BackPressureInfiniteWriter
class BackPressureStringFinitePipe[E, ZE, Config<:RedisConfig :Tag]
  extends Pipe[E with ZE with Config, ZE, String, String] {
  def readWithBackPressureRedis(stream: ZStream[ZE, Throwable, String]):
    ZIO[E with ZE with Config, Throwable, ZStream[ZE, Throwable, String]] =
    for {
      queueName <- ZIO.succeed(Random.alphanumeric.filter(_.isLetter).take(10).mkString)
      _ <- printLine(s"Reading stream with back pressure (using Redis queue ${queueName})")
      count <- {
        val r: Reader[Any, ZE, String] = Reader.UnitReader[Any, ZE, String](stream)
        val w: Pipe[Config, ZE, String, String] = ape.redis.Pipes.pipes[Config].generalPurpose[ZE].string(queueName)
        r --> w
      }.runCount
      s <- ape.redis.Readers.readers[Config].string(queueName, count.toInt).apply
    } yield s

  override protected[this] def pipe(i: ZStream[ZE, Throwable, String]):
    ZIO[E with ZE with Config, Throwable, ZStream[ZE, Throwable, String]] = readWithBackPressureRedis(i)
}
