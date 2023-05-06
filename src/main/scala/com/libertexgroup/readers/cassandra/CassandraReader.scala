package com.libertexgroup.readers.cassandra

import com.libertexgroup.ape.reader.Reader

import scala.reflect.ClassTag

abstract class CassandraReader[E, E1, T: ClassTag] extends Reader[E, E1, T] {
  override val name: String = "CassandraReader"
}
