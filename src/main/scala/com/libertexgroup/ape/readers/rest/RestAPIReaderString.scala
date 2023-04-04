
package com.libertexgroup.ape.readers.rest

import com.libertexgroup.ape.utils.RestUtils.sendRequestString
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

protected[readers] class RestAPIReaderString[ZE](
                                                   request: Request
                                               ) extends RestApiReader[Client,ZE,String] {

  override def apply: ZIO[Client, Throwable, ZStream[ZE, Throwable, String]] = sendRequestString(request)
}

