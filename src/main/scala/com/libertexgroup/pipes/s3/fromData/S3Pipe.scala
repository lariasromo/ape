package com.libertexgroup.pipes.s3.fromData

import com.libertexgroup.ape.pipe.Pipe

import scala.reflect.ClassTag


abstract class S3Pipe[E, E1, T :ClassTag, T2 :ClassTag] extends Pipe[E, E1, T, T2] {
  override val name: String = "S3Writer"
}