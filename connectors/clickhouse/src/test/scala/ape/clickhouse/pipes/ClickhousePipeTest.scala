package ape.clickhouse.pipes

import ape.clickhouse.configs.MultiClickhouseConfig
import ape.clickhouse.models.dummy
import ape.clickhouse.utils.ClickhouseContainerService
import ape.clickhouse.utils.ClickhouseJDBCUtils.query2ChunkMulti
import org.testcontainers.containers.ClickHouseContainer
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

//todo: this test is not passing
object ClickhousePipeTest extends ZIOSpec[MultiClickhouseConfig with ClickHouseContainer]  {
  val sampleRecords: Chunk[dummy] = Chunk(
    dummy("value1", "value2"),
    dummy("value3", "value4"),
    dummy("value5", "value6"),
    dummy("value7", "value8"),
    dummy("value9", "value10"),
  )

  val sampleData: ZStream[Any, Throwable, dummy] = ZStream.fromChunk(sampleRecords)

  def readsSampleData: ZIO[MultiClickhouseConfig, Any, Chunk[dummy]] = for {
    data <- query2ChunkMulti[dummy]("SELECT * FROM dummy;")
  } yield data

  val writer = ape.clickhouse.Pipes.pipes[MultiClickhouseConfig].default[Any, dummy]

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
