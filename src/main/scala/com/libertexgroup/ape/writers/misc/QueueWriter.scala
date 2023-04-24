package com.libertexgroup.ape.writers.misc

import com.libertexgroup.ape.Writer
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Queue, ZIO}

import scala.reflect.ClassTag

class QueueWriter[E, ET, T: ClassTag](queue: Queue[T]) extends Writer[E, ET, T, T] {
  override val name: String = "QueueWriter"

  override protected[this] def pipe(i: ZStream[ET, Throwable, T]): ZIO[E, Throwable, ZStream[ET, Throwable, T]] =
    ZIO.succeed{ i.tap(f => queue.offer(f)) }
}
