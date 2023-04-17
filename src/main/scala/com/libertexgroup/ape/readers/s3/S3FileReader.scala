package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader

import scala.reflect.ClassTag


abstract class S3FileReader[E, ZE, T :ClassTag] extends Reader[E, ZE, T]{
  override val name: String = "S3FileReader"
}
