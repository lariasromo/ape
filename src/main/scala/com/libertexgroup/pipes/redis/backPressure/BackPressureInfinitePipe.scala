package com.libertexgroup.pipes.redis.backPressure

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag

// Use with caution as this writer will produce an infinite stream, even if your input stream is finite.
// If working with a finite stream use BackPressureFiniteWriter
class BackPressureInfinitePipe[E, ZE, Config<:RedisConfig :Tag, T :SchemaFor :Encoder :Decoder :ClassTag :Tag]
  extends Pipe[E with ZE with Config, ZE, T, T] {
  def readWithBackPressureRedis(stream: ZStream[ZE, Throwable, T]):
    ZIO[E with ZE with Config, Throwable, ZStream[ZE, Throwable, T]] =
      for {
        rand <- ZIO.random
        queueName <- rand.nextString(10)
        _ <- printLine(s"Reading stream with back pressure (using Redis queue ${queueName})")
        _ <- {
          val r: Reader[Any, ZE, T] = Reader.UnitReader[Any, ZE, T](stream)
          val w: Pipe[Config, ZE, T, T] = Ape.pipes.redis[Config].generalPurpose[ZE].typed[T](queueName)
          r --> w
        }.runDrain.fork
        s <- Ape.readers.redis[Config].avro[T](queueName).apply
      } yield s

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]):
    ZIO[E with ZE with Config, Throwable, ZStream[ZE, Throwable, T]] = readWithBackPressureRedis(i)
}
