package ape.misc.pipes

import ape.pipe.Pipe
import zio.stream.ZStream
import zio.{Queue, ZIO}

import scala.reflect.ClassTag

class QueuePipe[E, ET, T: ClassTag](queue: Queue[T]) extends Pipe[E, ET, T, T] {
  override protected[this] def pipe(i: ZStream[ET, Throwable, T]): ZIO[E, Throwable, ZStream[ET, Throwable, T]] =
    ZIO.succeed{ i.tap(f => queue.offer(f)) }
}
