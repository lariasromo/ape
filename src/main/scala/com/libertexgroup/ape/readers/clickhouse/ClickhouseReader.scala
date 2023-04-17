package com.libertexgroup.ape.readers.clickhouse

import com.libertexgroup.ape.Reader

import scala.reflect.ClassTag

abstract class ClickhouseReader[E, E1, T: ClassTag] extends Reader[E, E1, T] {
  override val name: String = "ClickhouseReader"
}
