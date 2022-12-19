package com.libertexgroup.algebras.writers.clickhouse

import com.libertexgroup.algebras.writers.Writer
import com.libertexgroup.models.ClickhouseModel
import zio.ZIO
import zio.stream.ZStream

trait ClickhouseWriter[E] extends Writer[E] {
  type InputType = ClickhouseModel
  val sql: String
  override def apply(stream: ZStream[E, Throwable, InputType]): ZIO[EnvType, Any, Unit]
}
