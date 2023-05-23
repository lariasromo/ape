package ape.clickhouse.readers

import ape.clickhouse.configs.MultiClickhouseConfig
import ape.clickhouse.models.dummy
import ape.clickhouse.utils.ClickhouseContainerService
import org.testcontainers.containers.ClickHouseContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object ClickhouseReaderTest extends ZIOSpec[MultiClickhouseConfig with ClickHouseContainer] {
  val reader = ape.clickhouse.Readers.readers[MultiClickhouseConfig].default[dummy]("select * from dummy")

  override def spec: Spec[MultiClickhouseConfig with ClickHouseContainer with TestEnvironment with Scope, Any] =
    suite("ClickhouseReaderTest")(
      test("Reads dummy data"){
        for {
          _ <- ClickhouseContainerService.loadSampleData
          stream <- reader.apply
          data <- stream.runCollect
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.contains(dummy("value1", "value2")))
        }
      }
    )

  override def bootstrap: ZLayer[Any, Throwable, MultiClickhouseConfig with ClickHouseContainer] = ClickhouseContainerService.layer
}
