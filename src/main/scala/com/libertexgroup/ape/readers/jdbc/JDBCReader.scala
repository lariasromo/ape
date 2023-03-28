package com.libertexgroup.ape.readers.jdbc

import com.libertexgroup.ape.Reader

import scala.reflect.ClassTag

abstract class JDBCReader[E, E1, T :ClassTag] extends Reader[E, E1, T]