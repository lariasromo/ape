package com.libertexgroup.pipes.clickhouse

import com.libertexgroup.ape.pipe.Pipe

import scala.reflect.ClassTag

abstract class ClickhousePipe[E, ET, T:ClassTag, T1:ClassTag] extends Pipe[E, ET, T, T1]{
  override val name: String = "ClickhouseWriter"
}