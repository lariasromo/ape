
package com.libertexgroup.ape.readers.rest

import com.fxclub.commons.http.HttpUtil
import zio.{Chunk, ZIO}
import zio.http.{Client, Request, Response}
import zio.stream.ZStream

protected[readers] class RestAPIReaderString[E,T](
                                                 /* method: Method,
                                                  headers: List[(String,String)],
                                                  bodyString: String,
                                                  version: Version = Version.`HTTP/1.1`,
                                                  remoteAddress: Option[InetAddress] = Option.empty*/
                                               ) extends RestApiReader[Client,E,String] {



  def sendRequest(request: Request): ZIO[Client, Throwable, ZStream[Any, Throwable, String]] = {

    val response= for {
      responseFromHttpUtil <- HttpUtil.sentWithLogging(request)
      byteChunk <- responseFromHttpUtil.body.asStream.runCollect

    } yield {
      byteChunk.toArray.map(_.toChar).mkString

    }

    response.map(str => ZStream(str))
  }

  override def apply: ZIO[Client, Throwable, ZStream[E, Throwable, String]] = ???
}

