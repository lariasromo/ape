package com.libertexgroup.ape.pipes.jdbc

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.PostgresContainerService
import com.libertexgroup.ape.pipes.{sampleData, sampleRecords}
import com.libertexgroup.configs.JDBCConfig
import com.libertexgroup.utils.GenericJDBCUtils
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

object JDBCWriterTest  extends ZIOSpec[JDBCConfig with PostgreSQLContainer] {
  def readsSampleData: ZIO[JDBCConfig, Nothing, Chunk[dummy]] = for {
    data <- GenericJDBCUtils.query2Chunk[dummy, JDBCConfig]("SELECT * FROM dummy;")
  } yield data

  val writer = Ape.pipes.jdbc[JDBCConfig].default[Any, dummy]

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
