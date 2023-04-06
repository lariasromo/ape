package com.libertexgroup.ape.writers.misc

import com.libertexgroup.ape.Writer
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Queue, ZIO}

import scala.reflect.ClassTag

protected[misc] class QueueWriter[E, ET, T: ClassTag](queue: Queue[T]) extends Writer[E, ET, T, T] {
  override def apply(i: ZStream[ET, Throwable, T]): ZIO[E, Throwable, ZStream[ET, Throwable,T]] =
    ZIO.succeed{
      i.tap { file => printLine(s"Offering ${file} to queue") }.tap(f => queue.offer(f))
    }
}
