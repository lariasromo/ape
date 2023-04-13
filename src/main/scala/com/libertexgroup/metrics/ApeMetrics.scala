package com.libertexgroup.metrics

import zhttp.http._
import zhttp.service.Server
import zio.metrics._
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.{MetricsConfig, prometheus}
import zio.stream.ZStream
import zio.{ZIO, ZLayer, durationInt}


object ApeMetrics {
  private val metricsConfig = ZLayer.succeed(MetricsConfig(5.seconds))

  val publisherHttp: Http[PrometheusPublisher, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.GET -> !! / "metrics" =>
      ZIO.serviceWithZIO[PrometheusPublisher](
        _.get.map(Response.text),
      )
    }

  def apeCountPipe(pipeName: String): Metric[MetricKeyType.Counter, Any, MetricState.Counter] =
    Metric.counter("apeCountPipe").fromConst(1).tagged(MetricLabel("pipeName", pipeName))

  val apeCountAll: Metric[MetricKeyType.Counter, Any, MetricState.Counter] =
    Metric.counter("apeCountAll").fromConst(1)


  val serve: ZIO[Any, Nothing, Unit] = for {
    _ <- Server.start(8080, ApeMetrics.publisherHttp)
      .provide(
        // general config for all metric backend
        metricsConfig,
        // The prometheus reporting layer
        prometheus.publisherLayer,
        prometheus.prometheusLayer
      ).forkDaemon
  } yield ()

  val live: ZLayer[Any, Nothing, Unit] = ZLayer.scoped(serve)

  class StreamWithMetrics[ZE, T](stream: ZStream[ZE, Throwable, T]){
    def withMetrics(pipeName: String): ZStream[ZE, Throwable, T] = stream
      .tap(_ => ZIO.unit @@ ApeMetrics.apeCountPipe(pipeName))
      .tap(_ => ZIO.unit @@ ApeMetrics.apeCountAll)
  }

  implicit def streamWithMetrics[ZE, T]: ZStream[ZE, Throwable, T] => StreamWithMetrics[ZE, T] = s =>
    new StreamWithMetrics[ZE, T](s)
}
