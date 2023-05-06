package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.ClickhouseContainerService
import com.libertexgroup.ape.writers.{sampleData, sampleRecords}
import com.libertexgroup.configs.MultiClickhouseConfig
import com.libertexgroup.utils.ClickhouseJDBCUtils.query2ChunkMulti
import org.testcontainers.containers.ClickHouseContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

//todo: this test is not passing
object ClickhouseWriterTest extends ZIOSpec[MultiClickhouseConfig with ClickHouseContainer]  {
  def readsSampleData: ZIO[MultiClickhouseConfig, Any, Chunk[dummy]] = for {
    data <- query2ChunkMulti[dummy]("SELECT * FROM dummy;")
  } yield data

  val writer = Ape.pipes.clickhouse[MultiClickhouseConfig].default[Any, dummy]

  override def spec: Spec[MultiClickhouseConfig with ClickHouseContainer with TestEnvironment with Scope, Any] =
    suite("ClickhouseWriterTest")(
      test("Writes dummy data"){
        for {
          data <- readsSampleData
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.equals(sampleRecords))
        }
      }
    )

  val setup: ZIO[MultiClickhouseConfig, Throwable, Unit] = for {
    _ <- ClickhouseContainerService.createDummyTable
    _ <- writer.write(sampleData)
  } yield ()

  override def bootstrap: ZLayer[Any, Any, MultiClickhouseConfig with ClickHouseContainer] =
    ClickhouseContainerService.layer >+> ZLayer.fromZIO(setup)
}
