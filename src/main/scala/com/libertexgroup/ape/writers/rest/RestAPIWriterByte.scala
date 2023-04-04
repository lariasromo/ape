package com.libertexgroup.ape.writers.rest

import com.libertexgroup.ape.utils.RestUtils.sendRequestByte
import com.libertexgroup.ape.utils.reLayer
import zio.ZIO
import zio.http._
import zio.stream.ZStream


protected[readers] class RestAPIWriterByte[E] extends RestApiWriter[Client,E,Request, Byte] {

  override def apply(i: ZStream[E, Throwable, Request]): ZIO[Client, Throwable, ZStream[E, Throwable, Byte]] =
    for {
      l <- reLayer[Client]
      z = i.mapZIO(r => sendRequestByte(r).provideSomeLayer(l)).flatMap(x=>x)
    } yield z
}
