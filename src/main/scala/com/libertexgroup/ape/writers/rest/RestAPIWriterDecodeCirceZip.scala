
package com.libertexgroup.ape.writers.rest

import com.libertexgroup.ape.utils.RestUtils.sendRequestString
import com.libertexgroup.ape.utils.Utils.reLayer
import io.circe.{Decoder, jawn}
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[rest] class RestAPIWriterDecodeCirceZip[ZE, T:ClassTag, T2: ClassTag :Decoder](implicit request: T => Request)
  extends RestApiWriter[Client, ZE, T, (T, Option[T2])] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, (T, Option[T2])]] = for {
    l <- reLayer[Client]
  } yield i.mapZIO(r => for {
    respS <- sendRequestString(r).provideSomeLayer(l)
    resp <- respS.runCollect
  } yield (r, jawn.decode[T2](resp.mkString).toOption))
}
