package ape.jdbc.readers

import ape.reader.Reader

import scala.reflect.ClassTag

abstract class JDBCReader[E, E1, T :ClassTag] extends Reader[E, E1, T]