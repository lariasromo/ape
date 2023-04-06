package com.libertexgroup.ape.readers.clickhouse

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.ClickhouseContainerService
import com.libertexgroup.configs.MultiClickhouseConfig
import org.testcontainers.containers.ClickHouseContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object ClickhouseReaderTest extends ZIOSpec[MultiClickhouseConfig with ClickHouseContainer] {
  val reader = Ape.readers.clickhouse[MultiClickhouseConfig].default[Any, dummy]("select * from dummy")

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
