package com.libertexgroup.ape.readers.rest

import com.libertexgroup.ape.utils.RestUtils.sendRequestByte
import zio.ZIO
import zio.http._
import zio.stream.ZStream

protected[rest] class RestAPIReaderByte[E](
                                                 request: Request
                                           ) extends RestApiReader[Client,E,Byte] {

  override def apply: ZIO[Client, Throwable, ZStream[E, Throwable, Byte]] = sendRequestByte(request)
}
