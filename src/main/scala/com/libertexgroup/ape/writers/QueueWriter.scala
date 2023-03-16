package com.libertexgroup.ape.writers

import zio.Console.printLine
import zio.stream.{ZSink, ZStream}
import zio.{Queue, Scope, ZIO}

protected[writers] class QueueWriter[E, T](queue: Queue[T]) extends Writer[E, E, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E, Throwable, Unit] =
    stream
      .tap { file => printLine(s"Offering ${file} to queue") }
      .tap(f => queue.offer(f))
      .runDrain
}
