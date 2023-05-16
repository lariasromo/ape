# Metrics
By default, an APE pipeline collect metrics using ZMX with the Prometheus connector extension, it will expose all 
messages that are processed by each `Reader`/`Pipe`.

To expose metrics from your service containing an APE pipeline just include the `ApeMetrics` layer to your main 
program, you can do so by bootstrapping it with the method `ApeMetrics.live`

```scala
import com.libertexgroup.metrics.ApeMetrics
import zio.ZIOApp

object main extends ZIOApp {
  override def bootstrap = ApeMetrics.live

  override implicit def environmentTag: zio.EnvironmentTag[Environment] = zio.EnvironmentTag[Environment]
  override type Environment = Any
  override def run = ???
}
```

Metric server will be listening on port 8080 for as long the ZIO app runs, exposing metrics of all running APE 
pipelines. 

Use this guide to see how to stand up a prometheus and kibana servers to visualize metrics locally.
https://zio.github.io/zio-zmx/docs/metrics/metrics_prometheus#download-and-configure-prometheus