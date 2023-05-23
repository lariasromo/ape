package ape.redis.pipes.backPressure

import ape.pipe.Pipe
import ape.reader.Reader
import ape.redis.configs.RedisConfig
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Tag, ZIO}

// Use with caution as this writer will produce an infinite stream, even if your input stream is finite.
// If working with a finite stream use BackPressureFiniteWriter
class BackPressureInfiniteStringPipe[E, ZE, Config<:RedisConfig :Tag]
  extends Pipe[E with ZE with Config, ZE, String, String] {
  def readWithBackPressureRedis(stream: ZStream[ZE, Throwable, String]): ZIO[E with ZE with Config, Throwable, ZStream[ZE, Throwable, String]] =
    for {
      rand <- ZIO.random
      queueName <- rand.nextString(10)
      _ <- printLine(s"Reading stream with back pressure (using Redis queue ${queueName})")
      _ <- {
        val r: Reader[Any, ZE, String] = Reader.UnitReader[Any, ZE, String](stream)
        val w: Pipe[Config, ZE, String, String] = ape.redis.Pipes.pipes[Config].generalPurpose[ZE].string(queueName)
        r --> w
      }.runDrain.fork
      s <- ape.redis.Readers.readers[Config].avro[String](queueName).apply
    } yield s

  override protected[this] def pipe(i: ZStream[ZE, Throwable, String]):
    ZIO[E with ZE with Config, Throwable, ZStream[ZE, Throwable, String]] = readWithBackPressureRedis(i)
}
