package com.libertexgroup.algebras.writers.clickhouse

import com.libertexgroup.algebras.writers.Writer
import com.libertexgroup.models.ClickhouseModel
import zio.ZIO
import zio.stream.ZStream

trait ClickhouseWriter[E, E1, T] extends Writer[E, E1, T] {
  val sql: String
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E1, Throwable, Unit]
}
