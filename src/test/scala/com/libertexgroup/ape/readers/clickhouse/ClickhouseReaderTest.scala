package com.libertexgroup.ape.readers.clickhouse

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.readers.Reader
import com.libertexgroup.ape.utils.ClickhouseContainerService
import com.libertexgroup.configs.ClickhouseConfig
import org.testcontainers.containers.ClickHouseContainer
import zio.{Scope, ZIO, ZLayer}
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}

object ClickhouseReaderTest extends ZIOSpec[ClickhouseConfig with ClickHouseContainer] {
  val reader = Pipeline.readers.clickhouseDefaultReader[dummy]("select * from dummy")

  //new DefaultReader[Any, dummy]("select * from dummy").apply
  override def spec: Spec[ClickhouseConfig with ClickHouseContainer with TestEnvironment with Scope, Any] =
    suite("ClickhouseReaderTest")(
      test("Reads dummy data"){
        for {
          _ <- ClickhouseContainerService.loadSampleData
          stream <- reader.apply
          data <- stream.runCollect
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.head.equals(dummy("value1", "value2")))
        }
      }
    )

  override def bootstrap: ZLayer[Any, Throwable, ClickhouseConfig with ClickHouseContainer] = ClickhouseContainerService.layer
}
