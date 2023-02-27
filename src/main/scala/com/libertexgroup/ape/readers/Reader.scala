package com.libertexgroup.ape.readers

import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.transformers.DefaultTransformer
import com.libertexgroup.ape.writers.Writer
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

abstract class Reader[E, E1, T: ClassTag] {
  def apply: ZIO[E, Throwable, ZStream[E1, Throwable, T]]
  def -->[E2, T2: ClassTag](writer: Writer[E1, E2, T2])(implicit transform: T => T2): Pipeline[E, E1, T, T2, E2] =
    new Pipeline(this, new DefaultTransformer[E1, T, T2](), writer)
}
