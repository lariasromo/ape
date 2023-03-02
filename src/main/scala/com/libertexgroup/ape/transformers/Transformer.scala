package com.libertexgroup.ape.transformers

import zio.stream.ZStream

trait Transformer[E, I, O] {
  def apply(stream: ZStream[E, Throwable, I]): ZStream[E, Throwable, O]
}