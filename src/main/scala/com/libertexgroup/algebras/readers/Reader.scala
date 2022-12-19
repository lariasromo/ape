package com.libertexgroup.algebras.readers

import zio.ZIO
import zio.stream.ZStream

trait Reader {
  type Env
  type Env2
  type StreamType
  def apply: ZIO[Env, Throwable, ZStream[Env2, Throwable, StreamType]]
}
