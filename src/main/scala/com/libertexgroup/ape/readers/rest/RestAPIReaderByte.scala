package com.libertexgroup.ape.readers.rest

import java.net.InetAddress

import com.libertexgroup.ape.readers.Reader
import com.libertexgroup.configs.{HttpClientConfig, S3Config}
import com.fxclub.commons.http.HttpUtil
import zio.{ZIO, ZLayer}
import zio.stream.ZStream
import zio.http._
import zio.http.model.Headers.Header
import zio.http.model.{Headers, Method, Status, Version}


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
