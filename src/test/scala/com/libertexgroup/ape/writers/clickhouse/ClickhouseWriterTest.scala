package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.ClickhouseContainerService
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.query2Chunk
import com.libertexgroup.ape.writers.{Writer, sampleData, sampleRecords}
import com.libertexgroup.configs.ClickhouseConfig
import com.libertexgroup.models.clickhouse.ClickhouseModel
import org.testcontainers.containers.ClickHouseContainer
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

object ClickhouseWriterTest extends ZIOSpec[ClickhouseConfig with ClickHouseContainer]  {
  def readsSampleData: ZIO[ClickhouseConfig, Any, Chunk[dummy]] = for {
    data <- query2Chunk[dummy]("SELECT * FROM dummy;")
  } yield data

  val writer: Writer[Any, Any with Scope with ClickhouseConfig, ClickhouseModel] = Pipeline.writers.clickhouseWriter[Any]

  override def spec: Spec[ClickhouseConfig with ClickHouseContainer with TestEnvironment with Scope, Any] =
    suite("ClickhouseWriterTest")(
      test("Writes dummy data"){
        for {
          _ <- ClickhouseContainerService.createDummyTable
          _ <- writer.apply(sampleData)
          data <- readsSampleData
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.equals(sampleRecords))
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, ClickhouseConfig with ClickHouseContainer] = ClickhouseContainerService.layer
}
