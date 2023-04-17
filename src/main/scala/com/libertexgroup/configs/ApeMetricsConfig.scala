package com.libertexgroup.configs

import zio.System.envOrElse

case class ApeMetricsConfig(
                             port:Int,
                             path:String
                           )

object ApeMetricsConfig {
  def make = for {
    port <- envOrElse("APE_METRICS_PORT", "8080")
    path <- envOrElse("APE_METRICS_PATH", "metrics")
  } yield ApeMetricsConfig(
    port = port.toInt,
    path = path
  )
}
