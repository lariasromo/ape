package com.libertexgroup.ape.writers.cassandra

import com.libertexgroup.ape.Writer

import scala.reflect.ClassTag

abstract class CassandraWriter[E, E1, T :ClassTag, T2 :ClassTag] extends Writer[E, E1, T, T2] {
  override val name: String = "CassandraWriter"
}
