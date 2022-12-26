package com.libertexgroup.algebras.writers

import zio.{Has, ZIO}
import zio.stream.ZStream

trait Writer[E, E1, T] {
  def apply(stream: ZStream[E, Throwable, T]): ZIO[E1, Throwable, Unit]
}
