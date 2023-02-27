package com.libertexgroup.ape.readers.clickhouse

import com.libertexgroup.ape.readers.Reader

import scala.reflect.ClassTag

abstract class ClickhouseReader[E, E1, T: ClassTag] extends Reader[E, E1, T]
