package ape.cassandra.pipes

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.models.{LookupModel, dummy}
import ape.cassandra.utils.CassandraContainerService
import ape.cassandra.utils.CassandraContainerService.{containerLayer, makeCassandraConfig}
import ape.pipe.Pipe
import ape.reader.Reader
import com.dimafeng.testcontainers.CassandraContainer
import zio.Console.printLine
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZLayer}

object CassandraLookupPipeTest extends ZIOSpec[CassandraConfig with CassandraContainer] {
  val sampleRecords: Chunk[dummy] = Chunk(
    dummy("value1", "value2"),
    dummy("value2", "value4"),
    dummy("value2", "value6"),
    dummy("value1", "value7"),
    dummy("value1", "value77"),
    dummy("value2", "value8"),
    dummy("value2", "value10"),
  )
  val lookupRecords: Chunk[LookupModel] = Chunk(
    LookupModel("value1"), LookupModel("value2")
  )

  val sampleData: ZStream[Any, Throwable, dummy] = ZStream.fromChunk(sampleRecords)

  val writer = ape.cassandra.Pipes.pipes[CassandraConfig].default[Any, dummy]
  val lookupWriter = ape.cassandra.Pipes.pipes[CassandraConfig].lookup[Any, LookupModel, dummy]
  val reader = Reader.UnitReaderIter(lookupRecords)

  override def spec: Spec[CassandraConfig with CassandraContainer with TestEnvironment with Scope, Any] =
    suite("CassandraWriterTest")(
      test("Writes dummy data") {
        for {
          _ <- writer.write(sampleData)
          _ <- printLine("Done inserting")
          data <- (reader --> lookupWriter).runCollect
        } yield {
          val expectedData = Chunk(
            ( LookupModel("value1"), Chunk(
                dummy("value1", "value2"),
                dummy("value1", "value7"),
                dummy("value1", "value77")
              )
            ), (
              LookupModel("value2"), Chunk(
                dummy("value2", "value4"),
                dummy("value2", "value6"),
                dummy("value2", "value8"),
                dummy("value2", "value10")
            ) )
          )
          assertTrue(data.flatMap(c => c._2.map(d => s"${d.a}${d.b}")).sorted equals expectedData.flatMap(c => c._2.map(d => s"${d.a}${d.b}")).sorted)
        }
      }
    )

  val newLayer = for {
    cfg <- makeCassandraConfig
  } yield cfg.copy(batchSize = sampleRecords.length)
  
  override def bootstrap: ZLayer[Any, Throwable, CassandraContainer with CassandraConfig] =
    (containerLayer >+> ZLayer.fromZIO(newLayer)) >+> ZLayer.fromZIO(CassandraContainerService.createTable)
}
