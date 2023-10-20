package ape

import ape.pipe.Pipe
import ape.reader.Reader
import zio.Console.printLine
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

import scala.reflect.ClassTag

object WriterOpsTest extends ZIOSpec[Unit] {
  val reader = Reader.UnitReaderIter(Seq("file1", "file2", "file3", "file4", "file5"))
  val writer1 = Pipe.UnitZPipe[Any, Any, String, Unit](
    s => s.tap(i => printLine("InsertWithDlq: " + i) *> ZIO.succeed((1 to 1000000000).foreach(_ => ()))).map(_ => ())
  )

  val writer10 = Pipe.UnitZPipe[Any, Any, String, String](
    s => s.tap(i => printLine("Read: " + i))
  )
  val writer11 = Pipe.UnitZPipe[Any, Any, String, Unit](
    s => s.tap(i => printLine("Insert: " + i)).map(_ => ())
  )
  val writer12 = Pipe.UnitZPipe[Any, Any, Unit, Unit](
    s => s.tap(_ => printLine("Dlq ")).map(_ => ())
  )
  val writer2 = Pipe.UnitZPipe[Any, Any, String, Unit](
    s => s.tap(i => printLine("Counting: " + i)).map(_ => ())
  )

  val c2: Pipe[Any with Scope, Any, String, String] = (writer10 ++ (writer11 --> writer12)).map(_._1)
  val cL2: Pipe[Any with Scope, Any, String, Unit] = c2 --> writer2
  val cross: Pipe[Any with Scope, Any, String, Any] = writer1 <*> writer2
  val crossLeft: Pipe[Any with Scope, Any, String, Unit] = writer1 <* writer2
  val crossRight: Pipe[Any with Scope, Any, String, Unit] = writer1 *> writer2
  val zip: Pipe[Any with Scope, Any, String, (Unit, Unit)] = writer1 ++ writer2

  def runPipe[B: ClassTag](name: String, w: Pipe[Any with Scope, Any, String, B]): ZIO[Any, Throwable, Chunk[B]] = for {
    _ <- printLine(name)
    results <- ZIO.scoped((reader --> w).runCollect)
    _ <- results.mapZIO(r => printLine(r))
  } yield results

  val crossExpected = Chunk(
    (11, 21),
    (11, 22),
    (11, 23),
    (11, 24),
    (11, 25),
    (11, 26),
  )
  val crossLeftExpected = Chunk(
    11,
    11,
    11,
    11,
    11,
    11,
  )
  val crossRightExpected = Chunk(
    21,
    22,
    23,
    24,
    25,
    26,
  )
  val zipExpected = Chunk(
    (11, 21),
    (12, 22),
    (13, 23),
    (14, 24),
    (15, 25),
    (16, 26),
  )

  override def spec: Spec[Unit with TestEnvironment with Scope, Any] =
    suite("PipelineTest")(
      test("Simple pipeline with PipelineBuilder") {
        for {
          crossResults <- runPipe("cross", cross)
          crossLeftResults <- runPipe("crossLeft", crossLeft)
          crossLeft2Results <- runPipe("crossLeft2", cL2)
          crossRightResults <- runPipe("crossRight", crossRight)
          zipResults <- runPipe("zip", zip)
        } yield {
          assertTrue(crossResults.equals(crossExpected))
          assertTrue(crossLeftResults.equals(crossLeftExpected))
          assertTrue(crossLeft2Results.equals(crossLeftExpected))
          assertTrue(crossRightResults.equals(crossRightExpected))
          assertTrue(zipResults.equals(zipExpected))
          assertTrue(true)
        }
      },
    )

  override def bootstrap: ZLayer[Any, Throwable, Unit] = ZLayer.succeed()
}
