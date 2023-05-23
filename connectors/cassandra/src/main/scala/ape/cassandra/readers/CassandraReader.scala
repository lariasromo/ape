package ape.cassandra.readers

import ape.reader.Reader

import scala.reflect.ClassTag

abstract class CassandraReader[E, E1, T: ClassTag] extends Reader[E, E1, T]