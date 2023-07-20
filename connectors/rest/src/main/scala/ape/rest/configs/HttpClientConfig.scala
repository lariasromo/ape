package ape.rest.configs

import zio.System.envOrElse
import zio.http.{Path, URL}
import zio.{ZIO, ZLayer}
import zio.config.ZConfig
import zio.config.magnolia.descriptor

case class HttpClientConfig (
                              uri: String
                            ) {

  def getURL() =  new URL(Path.decode(uri))
}


object HttpClientConfig {
  val configDescriptor = descriptor[HttpClientConfig]
  val liveMagnolia: ZLayer[Any, Throwable, HttpClientConfig] = ZConfig.fromSystemEnv(configDescriptor)

  def make()  : ZIO[Any, SecurityException, HttpClientConfig] = for {
    uri <- envOrElse("URI", "")
  }yield {
    HttpClientConfig(uri)
  }

  def live: ZLayer[Any, SecurityException, HttpClientConfig] = ZLayer.fromZIO(make)
}


