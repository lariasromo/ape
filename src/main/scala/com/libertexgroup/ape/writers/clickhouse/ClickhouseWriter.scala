package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.ape.writers.Writer
import zio.{Chunk, ZIO}
import zio.stream.ZStream

trait ClickhouseWriter[E, E1, T] extends Writer[E, E1, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E1, Throwable, Unit]
}
