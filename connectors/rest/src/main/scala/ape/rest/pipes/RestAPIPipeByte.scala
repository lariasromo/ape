package ape.rest.pipes

import ape.rest.utils.RestUtils.sendRequestByte
import ape.utils.Utils.reLayer
import zio.ZIO
import zio.http._
import zio.stream.ZStream


protected[rest] class RestAPIPipeByte[ZE] extends RestApiPipe[Client,ZE,Request, Byte] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, Request]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, Byte]] = for {
    l <- reLayer[Client]
    z = i.mapZIO(r => sendRequestByte(r).provideSomeLayer(l)).flatMap(x=>x)
  } yield z
}
