
package ape.rest.pipes

import ape.rest.utils.RestUtils.sendRequestString
import ape.utils.Utils.reLayer
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

protected[rest] class RestAPIPipeString[ZE] extends RestApiPipe[Client, ZE, Request, String] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, Request]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, String]] = for {
    l <- reLayer[Client]
    z = i.mapZIO(r => sendRequestString(r).provideSomeLayer(l)).flatMap(x=>x)
  } yield z
}

