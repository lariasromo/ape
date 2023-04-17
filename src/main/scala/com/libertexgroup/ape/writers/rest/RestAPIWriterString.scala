
package com.libertexgroup.ape.writers.rest

import com.libertexgroup.ape.utils.RestUtils.sendRequestString
import com.libertexgroup.ape.utils.reLayer
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

protected[rest] class RestAPIWriterString[ZE] extends RestApiWriter[Client, ZE, Request, String] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, Request]):
    ZIO[Client, Throwable, ZStream[ZE, Throwable, String]] = for {
    l <- reLayer[Client]
    z = i.mapZIO(r => sendRequestString(r).provideSomeLayer(l)).flatMap(x=>x)
  } yield z
}

