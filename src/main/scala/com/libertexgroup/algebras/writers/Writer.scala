package com.libertexgroup.algebras.writers

import zio.{Has, ZIO}
import zio.stream.ZStream

trait Writer[E] {
  type EnvType
  type InputType
  def apply(stream: ZStream[E, Throwable, InputType]): ZIO[EnvType, Any, Unit]
}
