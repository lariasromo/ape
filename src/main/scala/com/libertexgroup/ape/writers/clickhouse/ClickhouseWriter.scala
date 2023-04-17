package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.ape.Writer

import scala.reflect.ClassTag

abstract class ClickhouseWriter[E, ET, T:ClassTag, T1:ClassTag] extends Writer[E, ET, T, T1]{
  override val name: String = "ClickhouseWriter"
}