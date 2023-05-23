package ape.metrics

import zio.System.envOrElse
import zio.ZIO

case class ApeMetricsConfig(
                             port:Int,
                             path:String
                           )

object ApeMetricsConfig {
  def make: ZIO[Any, SecurityException, ApeMetricsConfig] = for {
    port <- envOrElse("APE_METRICS_PORT", "8080")
    path <- envOrElse("APE_METRICS_PATH", "metrics")
  } yield ApeMetricsConfig(
    port = port.toInt,
    path = path
  )
}
