package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.Writer

import scala.reflect.ClassTag


abstract class S3Writer[E, E1, T :ClassTag, T2 :ClassTag] extends Writer[E, E1, T, T2] {
  override val name: String = "S3Writer"
}