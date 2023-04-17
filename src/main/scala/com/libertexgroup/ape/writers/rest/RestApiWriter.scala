package com.libertexgroup.ape.writers.rest

import com.libertexgroup.ape.Writer

import scala.reflect.ClassTag

abstract class RestApiWriter[E, E1, T :ClassTag, T2 :ClassTag] extends Writer[E, E1, T, T2] {
  override val name: String = "RestApiWriter"
}