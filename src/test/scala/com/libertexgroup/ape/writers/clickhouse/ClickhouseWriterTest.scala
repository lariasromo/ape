package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.ClickhouseContainerService
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.query2ChunkMulti
import com.libertexgroup.ape.writers.{Writer, sampleData, sampleRecords}
import com.libertexgroup.configs.MultiClickhouseConfig
import com.libertexgroup.models.clickhouse.ClickhouseModel
import org.testcontainers.containers.ClickHouseContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

object ClickhouseWriterTest extends ZIOSpec[MultiClickhouseConfig with ClickHouseContainer]  {
  def readsSampleData: ZIO[MultiClickhouseConfig, Any, Chunk[dummy]] = for {
    data <- query2ChunkMulti[dummy]("SELECT * FROM dummy;")
  } yield data

  val writer: Writer[Any, Any with Scope with MultiClickhouseConfig, ClickhouseModel] = Pipeline.writers.clickhouseWriter[Any]

  override def spec: Spec[MultiClickhouseConfig with ClickHouseContainer with TestEnvironment with Scope, Any] =
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

  override def bootstrap: ZLayer[Any, Any, MultiClickhouseConfig with ClickHouseContainer] = ClickhouseContainerService.layer
}
