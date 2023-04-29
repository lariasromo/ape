package com.libertexgroup.ape.writers.misc

import com.libertexgroup.ape.Writer
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Queue, ZIO}

import java.io.IOException
import scala.reflect.ClassTag

// Use with caution as this writer will produce an infinite stream, even if your input stream is finite.
// If working with a finite stream use BackPressureFiniteWriter
class BackPressureInfiniteWriter[ZE, T: ClassTag] extends Writer[ZE, ZE, T, T]{
  def readWithBackPressure(stream: ZStream[ZE, Throwable, T]): ZIO[ZE, Throwable, ZStream[Any, IOException, T]] =
    for {
      queue <- Queue.unbounded[T]
      rand <- ZIO.random
      queueName <- rand.nextPrintableChar
      _ <- printLine(s"Reading stream with back pressure (using queue ${queueName})")
      _ <- stream.tap(msg => queue.offer(msg)).runDrain.fork
    } yield ZStream.fromQueue(queue)
      .ensuring(queue.shutdown <* printLine(s"Shutting down queue ${queueName}").catchAll(_ => ZIO.unit))

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[ZE, Throwable, ZStream[ZE, Throwable, T]] =
    readWithBackPressure(i)
}
