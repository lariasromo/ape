package ape.cassandra.pipes

import ape.pipe.Pipe

import scala.reflect.ClassTag

abstract class CassandraPipe[E, E1, T :ClassTag, T2 :ClassTag] extends Pipe[E, E1, T, T2] {
  override val name: String = "CassandraWriter"
}
