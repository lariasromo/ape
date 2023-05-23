package ape.jdbc.readers

import ape.jdbc.configs.JDBCConfig
import ape.jdbc.models.dummy
import ape.jdbc.utils.PostgresContainerService
import com.dimafeng.testcontainers.PostgreSQLContainer
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object JDBCReaderTest extends ZIOSpec[JDBCConfig with PostgreSQLContainer] {
  val reader = ape.jdbc.Readers.readers[JDBCConfig].default[dummy]("select * from dummy")

  override def spec: Spec[JDBCConfig with PostgreSQLContainer with TestEnvironment with Scope, Any] = suite("JDBCReaderTest")(
    test("Reads dummy data") {
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
