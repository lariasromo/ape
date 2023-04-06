package com.libertexgroup.ape.readers.jdbc

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.PostgresContainerService
import com.libertexgroup.configs.JDBCConfig
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object JDBCReaderTest extends ZIOSpec[JDBCConfig with PostgreSQLContainer] {
  val reader = Ape.readers.jdbc.default[Any, dummy]("select * from dummy")

  override def spec: Spec[JDBCConfig with PostgreSQLContainer with TestEnvironment with Scope, Any] = suite("JDBCReaderTest")(
    test("Reads dummy data"){
      for {
        _ <- PostgresContainerService.loadSampleData
        stream <- reader.apply
        data <- stream.runCollect
      } yield {
        assertTrue(data.nonEmpty)
        assertTrue(data.contains(dummy("value1", "value2")))
      }
    }
  )

  override def bootstrap: ZLayer[Any, Any, JDBCConfig with PostgreSQLContainer] = PostgresContainerService.layer
}
