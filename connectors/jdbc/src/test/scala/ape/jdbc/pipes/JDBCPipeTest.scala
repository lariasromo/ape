package ape.jdbc.pipes

import ape.jdbc.configs.JDBCConfig
import ape.jdbc.models.dummy
import ape.jdbc.utils.{GenericJDBCUtils, PostgresContainerService}
import com.dimafeng.testcontainers.PostgreSQLContainer
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

object JDBCPipeTest  extends ZIOSpec[JDBCConfig with PostgreSQLContainer] {
  val sampleRecords: Chunk[dummy] = Chunk(
    dummy("value1", "value2"),
    dummy("value3", "value4"),
    dummy("value5", "value6"),
    dummy("value7", "value8"),
    dummy("value9", "value10"),
  )

  val sampleData: ZStream[Any, Throwable, dummy] = ZStream.fromChunk(sampleRecords)

  def readsSampleData: ZIO[JDBCConfig, Nothing, Chunk[dummy]] = for {
    data <- GenericJDBCUtils.query2Chunk[dummy, JDBCConfig]("SELECT * FROM dummy;")
  } yield data

  val writer = ape.jdbc.Pipes.pipes[JDBCConfig].default[Any, dummy]

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
