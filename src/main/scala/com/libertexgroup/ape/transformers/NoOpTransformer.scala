package com.libertexgroup.ape.transformers

import zio.stream.ZStream

// this transformer does nothing :)
// use it as a placeholder when reader output can be read by writer
class NoOpTransformer[E, T] extends Transformer[E, T, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZStream[E, Throwable, T] = stream
}
