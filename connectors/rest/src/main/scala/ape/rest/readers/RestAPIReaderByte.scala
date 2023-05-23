package ape.rest.readers

import ape.rest.utils.RestUtils.sendRequestByte
import zio.ZIO
import zio.http._
import zio.stream.ZStream

protected[rest] class RestAPIReaderByte[E](
                                                 request: Request
                                           ) extends RestApiReader[Client,E,Byte] {

  override protected[this] def read: ZIO[Client, Throwable, ZStream[E, Throwable, Byte]] = sendRequestByte(request)
}
