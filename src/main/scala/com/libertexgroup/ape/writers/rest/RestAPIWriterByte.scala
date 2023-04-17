package com.libertexgroup.ape.writers.rest

import com.libertexgroup.ape.utils.RestUtils.sendRequestByte
import com.libertexgroup.ape.utils.reLayer
import zio.ZIO
import zio.http._
import zio.stream.ZStream


protected[rest] class RestAPIWriterByte[ZE] extends RestApiWriter[Client,ZE,Request, Byte] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, Request]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, Byte]] = for {
    l <- reLayer[Client]
    z = i.mapZIO(r => sendRequestByte(r).provideSomeLayer(l)).flatMap(x=>x)
  } yield z
}
