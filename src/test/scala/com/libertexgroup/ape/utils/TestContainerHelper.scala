package com.libertexgroup.ape.utils

import zio.{Tag, Task, UIO, ZIO, ZLayer}

abstract class TestContainerHelper[F: Tag] {
  val startContainer: Task[F]
  val stopContainer: F => UIO[Unit]
  val layer: ZLayer[Any, Throwable, Any with F] = ZLayer.scoped {
    ZIO.acquireRelease(startContainer)(stopContainer)
  }
}
