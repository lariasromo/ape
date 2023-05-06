
package com.libertexgroup.pipes.rest

import com.libertexgroup.utils.RestUtils.sendRequestString
import com.libertexgroup.utils.Utils.reLayer
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[rest] class RestAPIPipeDecodeZip[ZE, T:ClassTag, T2: ClassTag]
(implicit request: T => Request, t2: String => T2) extends RestApiPipe[Client, ZE, T, (T, T2)] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, (T, T2)]] =
    for {
      l <- reLayer[Client]
    } yield i.mapZIO(r => for {
      respS <- sendRequestString(r).provideSomeLayer(l)
      resp <- respS.runCollect
    } yield (r, t2(resp.mkString)))
}
