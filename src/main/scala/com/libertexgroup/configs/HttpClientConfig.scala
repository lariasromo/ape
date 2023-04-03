package com.libertexgroup.configs


import zio.System.envOrElse
import zio.{ZIO, ZLayer}
import zio.http.{Path, URL}

case class HttpClientConfig (
                              uri: String
                            ) {

  def getURL() =  new URL(Path.decode(uri))
}


object HttpClientConfig extends ReaderConfig {
  def make()  : ZIO[Any, SecurityException, HttpClientConfig] = for {
    uri <- envOrElse("URI", "")
  }yield {
    HttpClientConfig(uri)
  }

  def live: ZLayer[Any, SecurityException, HttpClientConfig] = ZLayer.fromZIO(make)
}


