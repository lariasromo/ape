package ape.cassandra.readers

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.models.dummy
import ape.cassandra.utils.CassandraContainerService
import com.dimafeng.testcontainers.CassandraContainer
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZLayer}

class CassandraReaderTest extends ZIOSpec[CassandraConfig with CassandraContainer] {
  val sampleRecords: Chunk[dummy] = Chunk(
    dummy("value1", "value2"),
    dummy("value3", "value4"),
    dummy("value5", "value6"),
    dummy("value7", "value8"),
    dummy("value9", "value10"),
  )

  val sampleData: ZStream[Any, Throwable, dummy] = ZStream.fromChunk(sampleRecords)

  val writer = ape.cassandra.Pipes.pipes[CassandraConfig].default[Any, dummy]
  val reader = ape.cassandra.Readers.readers[CassandraConfig].default[dummy]("select * from dummy")

  override def spec: Spec[CassandraConfig with CassandraContainer with TestEnvironment with Scope, Any] =
    suite("CassandraWriterTest")(
      test("Writes dummy data") {
        for {
          _ <- writer.write(sampleData)
          expectedData <- sampleData.runCollect
          data <- reader.stream.runCollect
        } yield {
          assertTrue(data.map(d => s"${d.a}${d.b}").sorted equals expectedData.map(d => s"${d.a}${d.b}").sorted)
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, CassandraConfig with CassandraContainer] =
    CassandraContainerService.layer >+> ZLayer.fromZIO(CassandraContainerService.createTable)
}
