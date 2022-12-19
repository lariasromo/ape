package com.libertexgroup.algebras.transformers

import zio.stream.ZStream

trait Transformer[E, I] {
  type OutputType
  def apply(stream: ZStream[E, Throwable, I]): ZStream[E, Throwable, OutputType]
}
