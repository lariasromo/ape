package com.libertexgroup.ape.transformers

import zio.stream.ZStream

class NoOpTransformer[E, T] extends Transformer[E, T, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZStream[E, Throwable, T] = stream
}
