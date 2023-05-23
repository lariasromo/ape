package ape.rest.utils

import com.fxclub.commons.http.HttpUtil
import zio.ZIO
import zio.http.{Client, Request}
import zio.stream.ZStream

object RestUtils {
  def sendRequestByte[ZE](request: Request): ZIO[Client, Throwable, ZStream[ZE, Throwable, Byte]] = {

    val response = for {
      response <- HttpUtil.sentWithLogging(request)
    } yield {
      response
    }
    response.map { res => res.body.asStream }
  }

  def sendRequestString[ZE](request: Request): ZIO[Client, Throwable, ZStream[ZE, Throwable, String]] = {

    val response = for {
      responseFromHttpUtil <- HttpUtil.sentWithLogging(request)
      byteChunk <- responseFromHttpUtil.body.asStream.runCollect

    } yield {
      byteChunk.toArray.map(_.toChar).mkString

    }

    response.map(str => ZStream(str))
  }
}
