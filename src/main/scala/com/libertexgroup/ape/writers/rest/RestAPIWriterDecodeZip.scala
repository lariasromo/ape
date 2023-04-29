
package com.libertexgroup.ape.writers.rest

import com.libertexgroup.ape.utils.RestUtils.sendRequestString
import com.libertexgroup.ape.utils.Utils.reLayer
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[rest] class RestAPIWriterDecodeZip[ZE, T:ClassTag, T2: ClassTag]
(implicit request: T => Request, t2: String => T2) extends RestApiWriter[Client, ZE, T, (T, T2)] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, (T, T2)]] =
    for {
      l <- reLayer[Client]
    } yield i.mapZIO(r => for {
      respS <- sendRequestString(r).provideSomeLayer(l)
      resp <- respS.runCollect
    } yield (r, t2(resp.mkString)))
}
