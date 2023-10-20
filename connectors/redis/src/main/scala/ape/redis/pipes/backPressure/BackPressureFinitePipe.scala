package ape.redis.pipes.backPressure

import ape.pipe.Pipe
import ape.reader.Reader
import ape.redis.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag
import scala.util.Random

// Use with caution as this writer will consume stream first then produce a new stream
// So the first stream needs to be finite, if reading from an infinite stream, use BackPressureInfiniteWriter
class BackPressureFinitePipe[E, ZE, Config<:RedisConfig :Tag, T :SchemaFor :Encoder :Decoder :ClassTag :Tag]
  extends Pipe[E with ZE with Config, ZE, T, T] {
  def readWithBackPressureRedis(stream: ZStream[ZE, Throwable, T]):
    ZIO[E with ZE with Config, Throwable, ZStream[ZE, Throwable, T]] =
    for {
      queueName <- ZIO.succeed(Random.alphanumeric.filter(_.isLetter).take(10).mkString)
      _ <- ZIO.logInfo(s"Reading stream with back pressure (using Redis queue ${queueName})")
      count <- {
        val r: Reader[Any, ZE, T] = Reader.UnitReaderStream[Any, ZE, T](stream)
        val w: Pipe[Config, ZE, T, T] = ape.redis.Pipes.pipes[Config].generalPurpose[ZE].typed[T](queueName)
        r --> w
      }.runCount
      s <- ape.redis.Readers.readers[Config].avro[T](queueName, count.toInt).apply
    } yield s

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]):
    ZIO[E with ZE with Config, Throwable, ZStream[ZE, Throwable, T]] = readWithBackPressureRedis(i)
}
