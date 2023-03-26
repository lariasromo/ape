package com.libertexgroup.ape.writers.jdbc

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.GenericJDBCUtils.query2Chunk
import com.libertexgroup.ape.utils.PostgresContainerService
import com.libertexgroup.ape.writers.{sampleData, sampleRecords}
import com.libertexgroup.configs.JDBCConfig
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

object JDBCWriterTest  extends ZIOSpec[JDBCConfig with PostgreSQLContainer] {
  def readsSampleData: ZIO[JDBCConfig, Nothing, Chunk[dummy]] = for {
    data <- query2Chunk[dummy]("SELECT * FROM dummy;")
  } yield data

  val writer = Ape.writers.jDBCWriter[Any]

  override def spec: Spec[JDBCConfig with PostgreSQLContainer with TestEnvironment with Scope, Any] =
    suite("JDBCWriterTest")(
      test("Writes dummy data"){
        for {
          data <- readsSampleData
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.equals(sampleRecords))
        }
      }
    )

  val setup: ZIO[JDBCConfig, Throwable, Unit] = for {
    _ <- PostgresContainerService.createDummyTable
    _ <- writer.write(sampleData)
  } yield ()

  override def bootstrap: ZLayer[Any, Any, JDBCConfig with PostgreSQLContainer] =
    PostgresContainerService.layer >+> ZLayer.fromZIO(setup)
}
