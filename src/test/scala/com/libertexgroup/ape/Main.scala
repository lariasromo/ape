package com.libertexgroup.ape

import com.libertexgroup.configs.{ClickhouseConfig, MultiClickhouseConfig}
import zio.{ZIO, ZIOAppDefault}




object Main extends ZIOAppDefault {
  val test = for {
    conf <- ZIO.service[MultiClickhouseConfig]
  } yield ()
  override def run = test.provideSomeLayer(
    ClickhouseConfig.live() >+> MultiClickhouseConfig.liveFromNode
  )
}
