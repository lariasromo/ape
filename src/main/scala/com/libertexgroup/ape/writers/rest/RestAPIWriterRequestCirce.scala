package com.libertexgroup.ape.writers.rest

import scala.reflect.ClassTag
import com.libertexgroup.ape.utils.RestUtils.sendRequestString
import com.libertexgroup.ape.utils.reLayer
import io.circe.{Decoder, jawn}
import zio.{ULayer, ZIO}
import zio.http.{Client, Request}
import zio.stream.ZStream

class RestAPIWriterRequestCirce[E, ZE, T:ClassTag, T2: ClassTag :Decoder]
  extends RestApiWriter[E with Client, ZE, (Request,T),(T,Option[T2])] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, (Request, T)]):
    ZIO[E with Client, Throwable, ZStream[ZE, Throwable, (T, Option[T2])]] = for {
    l <- reLayer[Client]
  } yield { i.mapZIO { case (r, t) => for {
    respS <- sendRequestString(r).provideSomeLayer(l)
    resp <- respS.runCollect
  } yield (t, jawn.decode[T2](resp.mkString).toOption)
  }}
}
