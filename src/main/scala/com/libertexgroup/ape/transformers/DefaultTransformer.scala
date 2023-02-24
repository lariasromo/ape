package com.libertexgroup.ape.transformers
import zio.stream.ZStream

class DefaultTransformer[E, A, B](implicit algebra: A => B) extends Transformer[E, A, B] {
  override def apply(stream: ZStream[E, Throwable, A]): ZStream[E, Throwable, B] = stream map algebra
}
