
package com.libertexgroup.pipes.rest

import com.libertexgroup.utils.RestUtils.sendRequestString
import com.libertexgroup.utils.Utils.reLayer
import io.circe.{Decoder, jawn}
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[rest] class RestAPIPipeDecodeCirce[ZE, T: Decoder : ClassTag] extends RestApiPipe[Client, ZE, Request, T]
 {
   override protected[this] def pipe(i: ZStream[ZE, Throwable, Request]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, T]] = for {
     l <- reLayer[Client]
     z = i.mapZIO(r => sendRequestString(r).provideSomeLayer(l))
       .flatMap(x=>x).map(jawn.decode[T](_).toOption)
       .filter(_.isDefined).map(_.get)
   } yield z
 }

