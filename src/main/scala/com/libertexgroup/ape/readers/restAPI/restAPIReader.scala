package com.libertexgroup.ape.readers.restAPI

import java.net.InetAddress

import com.libertexgroup.ape.readers.Reader
import com.libertexgroup.configs.{HttpClientConfig, S3Config}
import com.fxclub.commons.http.HttpUtil
import zio.ZIO
import zio.stream.ZStream
import zio.http._
import zio.http.model.Headers.Header
import zio.http.model.{Headers, Method, Status, Version}


protected[readers] class restAPIReader[E,T](
                                             method: Method,
                                             headers: List[(String,String)],
                                             bodyString: String,
                                             version: Version = Version.`HTTP/1.1`,
                                             remoteAddress: Option[InetAddress] = Option.empty
                                           ) extends abstractRestApiReader[HttpClientConfig,E,T] {


  def createHeadersFromListTuple(headersList: List[(String,String)) = createHeadersFromListTupleHelper(headersList,Headers.empty)
  def createHeadersFromListTupleHelper(headersList: List[(String,String)], headers: Headers): Headers = {
    if (headersList.isEmpty) headers
    else headers.++( Header(headersList.head._1,headersList.head._2)).combine(createHeadersFromListTupleHelper(headersList.tail,headers))
  }

  override def apply(): ZIO[HttpClientConfig, Throwable, ZStream[E, Throwable, T]] = {







    val response: ZIO[Client with HttpClientConfig, Throwable, Response] = for {
      config <- ZIO.service[HttpClientConfig]
      val request = Request(
        Body.fromString(bodyString),
        createHeadersFromListTuple( headers),
        method,
        config.getURL(),
        version,
        remoteAddress)
      response <- HttpUtil.sentWithLogging(request, requestId = ZIO.succeed("myId"))
    } yield {
      response
    }


  response.map{
    res => res.body
  }



  }
}
