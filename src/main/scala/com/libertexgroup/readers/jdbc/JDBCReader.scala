package com.libertexgroup.readers.jdbc

import com.libertexgroup.ape.reader.Reader

import scala.reflect.ClassTag

abstract class JDBCReader[E, E1, T :ClassTag] extends Reader[E, E1, T] {
  override val name: String = "JDBCReader"
}