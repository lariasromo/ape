package com.libertexgroup.ape.readers.cassandra

import com.libertexgroup.ape.Reader

import scala.reflect.ClassTag

abstract class CassandraReader[E, E1, T: ClassTag] extends Reader[E, E1, T] {
  override val name: String = "CassandraReader"
}
