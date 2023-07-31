package ape.rest.pipes

import ape.rest.utils.RestUtils.sendRequestString
import ape.utils.Utils.reLayer
import io.circe.{Decoder, jawn}
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

import scala.reflect.ClassTag

class RestAPIPipeRequestString[ZE, T:ClassTag]
  extends RestApiPipe[Client, ZE, (Request,T),(T,String)] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, (Request, T)]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, (T, String)]] = for {
    l <- reLayer[Client]
  } yield { i.mapZIO { case (r, t) => for {
    respS <- sendRequestString(r).provideSomeLayer(l)
    resp <- respS.runCollect
  } yield (t, resp.mkString)
  }}
}
