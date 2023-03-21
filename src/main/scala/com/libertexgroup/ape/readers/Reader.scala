package com.libertexgroup.ape.readers

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.transformers.{DefaultTransformer, NoOpTransformer, Transformer}
import com.libertexgroup.ape.writers.{PipelineWriters, Writer}
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

abstract class Reader[E, E1, T: ClassTag] {
  def apply: ZIO[E, Throwable, ZStream[E1, Throwable, T]]

  def -->[T2: ClassTag, E2](writer: Writer[E1, E2, T2])(implicit transform: T => T2): Ape[E, E1, T, T2, E2] =
    new Ape(this, new DefaultTransformer[E1, T, T2](), writer)

  def -+>[T2: ClassTag, E2](writer: Writer[E1, E2, T2])
                           (implicit transformer: Transformer[E1, T, T2]): Ape[E, E1, T, T2, E2] =
    new Ape(this, transformer, writer)

  def ->[E2](writer: Writer[E1, E2, T]): Ape[E, E1, T, T, E2] =
    new Ape[E, E1, T, T, E2](this, new NoOpTransformer[E1, T](), writer)

  def ++[EE, EE1, TT: ClassTag, TTT: ClassTag](newReader: Reader[EE, EE1, TT])
  (implicit streamTransformer: (ZStream[E1, Throwable, T], ZStream[EE1, Throwable, TT]) => ZStream[E1 with EE1, Throwable, TTT]):
  Reader[E with EE, E1 with EE1, TTT] = {
      new ReaderUnion[E, E1, T, EE, EE1, TT, TTT](this, newReader, streamTransformer)
  }
}
