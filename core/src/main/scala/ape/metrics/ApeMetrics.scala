package ape.metrics

import zio._
import zio.http._
import zio.http.model.Method
import zio.metrics._
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.{MetricsConfig, prometheus}
import zio.stream.ZStream

import java.net.InetSocketAddress


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

  val healthApp: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> !! / "live" => Response.text("It's Alive!")
  }

  def app(path:String): Http[PrometheusPublisher, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.GET -> !! / "metrics" =>
      ZIO.serviceWithZIO[PrometheusPublisher](
        _.get.map(Response.text),
      )
    }

  val serve: ZIO[ApeMetricsConfig with Scope, Throwable, Fiber.Runtime[Throwable, Nothing]] =
    ZIO.acquireRelease(
      for {
        conf <- ZIO.service[ApeMetricsConfig]
        _ <- ZIO.logInfo(s"Starting metrics server on port ${conf.port} and path: /${conf.path}")
        server <- Server.serve((app(conf.path) ++ healthApp).withDefaultErrorResponse)
          .provide(
            // general config for all metric backend
            metricsConfig,
            // The prometheus reporting layer
            prometheus.publisherLayer,
            prometheus.prometheusLayer,
            ZLayer.succeed(ServerConfig(address = new InetSocketAddress(conf.port))) >>> Server.live
          )
          .onInterrupt(ZIO.logError("Metrics server interrupted"))
          .catchAll(ex => for {
            _ <- ZIO.logError("Something happened with the metrics server " + ex.getMessage)
          } yield throw new Exception(ex))
          .fork
      } yield server
    )(s => for {
      _ <- ZIO.logError("Metrics server stopped")
      _ <- s.interruptFork
    } yield ())

  val makeConfig: ZIO[Any, Throwable, ApeMetricsConfig] = ApeMetricsConfig.make
  val liveConfig: ZLayer[Any, Throwable, ApeMetricsConfig] = ZLayer.fromZIO(makeConfig)
  val livePublisher: ZLayer[ApeMetricsConfig, Throwable, Fiber.Runtime[Throwable, Nothing]] = ZLayer.scoped(serve)
  val live: ZLayer[Any, Throwable, Fiber.Runtime[Throwable, Nothing]] = liveConfig >>> livePublisher

  class StreamWithMetrics[ZE, T](stream: ZStream[ZE, Throwable, T]){
    def withMetrics(pipeName: String): ZStream[ZE, Throwable, T] = stream
      .tap(_ => ZIO.unit @@ ApeMetrics.apeCountPipe(pipeName))
      .tap(_ => ZIO.unit @@ ApeMetrics.apeCountAll)
  }

  implicit def streamWithMetrics[ZE, T]: ZStream[ZE, Throwable, T] => StreamWithMetrics[ZE, T] = s =>
    new StreamWithMetrics[ZE, T](s)
}
