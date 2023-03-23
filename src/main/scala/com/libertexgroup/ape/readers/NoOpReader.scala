package com.libertexgroup.ape.readers

import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

protected [readers] class NoOpReader[E, T: ClassTag] (stream: ZStream[E, Throwable, T]) extends Reader[Any, E, T]{
  override def apply: ZIO[Any, Throwable, ZStream[E, Throwable, T]] = ZIO.succeed(stream)
}
