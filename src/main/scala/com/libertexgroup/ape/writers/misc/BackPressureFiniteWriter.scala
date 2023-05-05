package com.libertexgroup.ape.writers.misc

import com.libertexgroup.ape.Writer
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Queue, ZIO}

import java.io.IOException
import scala.reflect.ClassTag

// Use with caution as this writer will consume stream first then produce a new stream
// So the first stream needs to be finite, if reading from an infinite stream use BackPressureInfiniteWriter
class BackPressureFiniteWriter[ZE, T: ClassTag] extends Writer[ZE, ZE, T, T]{
  def readWithBackPressure(stream: ZStream[ZE, Throwable, T]): ZIO[ZE, Throwable, ZStream[Any, IOException, T]] =
    for {
      queue <- Queue.unbounded[T]
      rand <- ZIO.random
      queueName <- rand.nextPrintableChar
      _ <- printLine(s"Reading stream with back pressure (using queue ${queueName})")
      count <- stream.tap(msg => queue.offer(msg)).runCount
    } yield ZStream.range(0, count.toInt)
      .mapZIO(_ => queue.take)
      .ensuring(queue.shutdown <* printLine(s"Shutting down queue ${queueName}").catchAll(_ => ZIO.unit))

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[ZE, Throwable, ZStream[ZE, Throwable, T]] =
    readWithBackPressure(i)
}