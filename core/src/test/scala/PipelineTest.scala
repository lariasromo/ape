import ape.models.dummy
import ape.pipe.Pipe
import ape.reader.Reader
import zio.Config.LocalDate
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, TestResult, ZIOSpecDefault, assertTrue}
import zio.{Chunk, Scope, ZIO}

import java.time.ZoneId


object PipelineTest extends ZIOSpecDefault {
  val test: ZIO[Any, Nothing, TestResult] = for {
    testResult <- ZIO.succeed(true)
  } yield {
    assertTrue(testResult)
  }
  val sampleDataOptions: ZStream[Any, Nothing, Option[dummy]] = ZStream.fromChunk(
    Chunk(
      Some(dummy("value1", "value2")),
      Some(dummy("value2", "value3")),
      None,
      Some(dummy("value3", "value4")),
      None,
    )
  )
  val reader: Reader[Any, Any, Option[dummy]] = Reader.UnitReader[Any, Any, Option[dummy]](sampleDataOptions)
  val dummyT: Option[dummy] => Option[dummy] = d => d
  val sampleDataNoOp: Chunk[dummy] = Chunk(
    dummy("value1", "value2"),
    dummy("value2", "value3"),
    dummy("value3", "value4")
  )
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("PipelineTest")(
    test("Hello world") {
      for {
        testResult <- ZIO.succeed(true)
      } yield {
        assertTrue(testResult)
      }
    },
    test("Reader safeGet") {
      for {
        testResult <- reader.safeGet[dummy].apply
        chunkResult <- testResult.runCollect
      } yield {
        assertTrue(chunkResult.equals(sampleDataNoOp))
      }
    },
    test("Writer safeGet") {
      for {
        chunkResult <- (reader --> Pipe.UnitTWriter[Any, Any, Option[dummy], Option[dummy]](dummyT).safeGet[dummy]).runCollect
      } yield {
        assertTrue(chunkResult.equals(sampleDataNoOp))
      }
    },
    test("Class names") {
      val dt = LocalDate.parse("2023-03-01").map(dt => dt.atStartOfDay(ZoneId.of("UTC")))
      assertTrue(true)
    })
}
