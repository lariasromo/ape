package com.libertexgroup.ape

import zio.{Tag, ULayer, ZIO, ZLayer}

package object utils {
  def reLayer[T :Tag]: ZIO[T, Nothing, ULayer[T]] = for {
    t <- ZIO.service[T]
  } yield ZLayer.succeed(t)
}
