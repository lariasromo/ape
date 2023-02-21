package com.libertexgroup.ape.readers

import zio.ZIO
import zio.stream.ZStream

trait Reader[E, E1, T] {
  def apply: ZIO[E, Throwable, ZStream[E1, Throwable, T]]
}
