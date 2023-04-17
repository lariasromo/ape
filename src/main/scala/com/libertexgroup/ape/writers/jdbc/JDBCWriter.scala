package com.libertexgroup.ape.writers.jdbc

import com.libertexgroup.ape.Writer

import scala.reflect.ClassTag

abstract class JDBCWriter[E, E1, T :ClassTag, T2 :ClassTag] extends Writer[E, E1, T, T2]{
  override val name: String = "JDBCWriter"
}
