package com.libertexgroup.ape.readers.rest

import com.fxclub.commons.http.HttpUtil
import zio.ZIO
import zio.http._
import zio.stream.ZStream


protected[readers] class RestAPIReaderByte[E,T](
                                           ) extends RestApiReader[Client,E,Byte] {


   def sendRequest(request: Request): ZIO[Client, Throwable, ZStream[Any, Throwable, Byte]] = {

    val response = for {
     response <- HttpUtil.sentWithLogging(request)
    } yield {
      response
    }
    response.map{res =>res.body.asStream}
  }

  override def apply: ZIO[Client, Throwable, ZStream[E, Throwable, Byte]] = ???
}
