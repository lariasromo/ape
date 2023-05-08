package com.libertexgroup.utils

import zio.{Tag, ULayer, ZIO, ZLayer}

object Utils {
  def reLayer[T :Tag]: ZIO[T, Nothing, ULayer[T]] = for {
    t <- ZIO.service[T]
  } yield ZLayer.succeed(t)

  class :=[T,Q]
  trait Default_:={
    /** Ignore default */
    implicit def useProvided[Provided,Default] = new :=[Provided,Default]
  }
  object := extends Default_:={
    /** Infer type argument to default */
    implicit def useDefault[Default] = new :=[Default,Default]
  }
}
