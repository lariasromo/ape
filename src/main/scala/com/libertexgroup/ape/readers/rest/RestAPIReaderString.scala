
package com.libertexgroup.ape.readers.rest

import com.fxclub.commons.http.HttpUtil
import zio.{Chunk, ZIO}
import zio.http.{Client, Request, Response}
import zio.stream.ZStream

protected[readers] class RestAPIReaderString[E](
                                                   request: Request
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

  override def apply = {
    val response= for {
      responseFromHttpUtil <- HttpUtil.sentWithLogging(request)
      byteChunk <- responseFromHttpUtil.body.asStream.runCollect

    } yield {
      byteChunk.toArray.map(_.toChar).mkString

    }

    response.map(str => ZStream(str))
  }
}

