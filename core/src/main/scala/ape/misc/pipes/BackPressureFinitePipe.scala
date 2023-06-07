package ape.misc.pipes

import ape.pipe.Pipe
import zio.stream.ZStream
import zio.{Queue, ZIO}

import java.io.IOException
import scala.reflect.ClassTag

// Use with caution as this writer will consume stream first then produce a new stream
// So the first stream needs to be finite, if reading from an infinite stream use BackPressureInfiniteWriter
class BackPressureFinitePipe[ZE, T: ClassTag] extends Pipe[ZE, ZE, T, T]{
  def readWithBackPressure(stream: ZStream[ZE, Throwable, T]): ZIO[ZE, Throwable, ZStream[Any, IOException, T]] =
    for {
      queue <- Queue.unbounded[T]
      rand <- ZIO.random
      queueName <- rand.nextPrintableChar
      _ <- ZIO.logInfo(s"Reading stream with back pressure (using queue ${queueName})")
      count <- stream.tap(msg => queue.offer(msg)).runCount
    } yield ZStream.range(0, count.toInt)
      .mapZIO(_ => queue.take)
      .ensuring(queue.shutdown <* ZIO.logInfo(s"Shutting down queue ${queueName}"))

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[ZE, Throwable, ZStream[ZE, Throwable, T]] =
    readWithBackPressure(i)
}
