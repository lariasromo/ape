
package com.libertexgroup.ape.readers.rest

import com.libertexgroup.ape.utils.RestUtils.sendRequestString
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

protected[rest] class RestAPIReaderString[ZE](
                                                   request: Request
                                               ) extends RestApiReader[Client,ZE,String] {

  override protected[this] def read: ZIO[Client, Throwable, ZStream[ZE, Throwable, String]] = sendRequestString(request)
}

