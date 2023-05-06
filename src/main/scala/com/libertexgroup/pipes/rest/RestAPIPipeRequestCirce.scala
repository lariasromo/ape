package com.libertexgroup.pipes.rest

import com.libertexgroup.utils.RestUtils.sendRequestString
import com.libertexgroup.utils.Utils.reLayer
import io.circe.{Decoder, jawn}
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

import scala.reflect.ClassTag

class RestAPIPipeRequestCirce[E, ZE, T:ClassTag, T2: ClassTag :Decoder]
  extends RestApiPipe[E with Client, ZE, (Request,T),(T,Option[T2])] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, (Request, T)]):
    ZIO[E with Client, Throwable, ZStream[ZE, Throwable, (T, Option[T2])]] = for {
    l <- reLayer[Client]
  } yield { i.mapZIO { case (r, t) => for {
    respS <- sendRequestString(r).provideSomeLayer(l)
    resp <- respS.runCollect
  } yield (t, jawn.decode[T2](resp.mkString).toOption)
  }}
}
