package ape.clickhouse.readers

import ape.reader.Reader

import scala.reflect.ClassTag

abstract class ClickhouseReader[E, E1, T: ClassTag] extends Reader[E, E1, T]