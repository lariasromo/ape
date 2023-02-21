package com.libertexgroup.ape.readers.jdbc

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.PostgresContainerService
import com.libertexgroup.configs.JDBCConfig
import zio.{Scope, ZLayer}
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}

object JDBCReaderTest extends ZIOSpec[JDBCConfig with PostgreSQLContainer] {
  override def spec: Spec[JDBCConfig with PostgreSQLContainer with TestEnvironment with Scope, Any] = suite("JDBCReaderTest")(
    test("Reads dummy data"){
      for {
        _ <- PostgresContainerService.loadSampleData
        stream <- new DefaultReader[dummy]("select * from dummy").apply
        data <- stream.runCollect
      } yield {
        assertTrue(data.nonEmpty)
        assertTrue(data.head.equals(dummy("value1", "value2")))
      }
    }
  )

  override def bootstrap: ZLayer[Any, Any, JDBCConfig with PostgreSQLContainer] = PostgresContainerService.layer
}
