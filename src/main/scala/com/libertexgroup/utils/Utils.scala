package com.libertexgroup.utils

import zio.{Tag, ULayer, ZIO, ZLayer}

object Utils {
  def reLayer[T :Tag]: ZIO[T, Nothing, ULayer[T]] = for {
    t <- ZIO.service[T]
  } yield ZLayer.succeed(t)
}
