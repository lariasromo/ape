package com.libertexgroup.ape.pipelines

import com.libertexgroup.ape.{Reader, Writer}
import zio.Console.printLine
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

import scala.reflect.ClassTag

object WriterOpsTest extends ZIOSpec[Unit] {
  val stream = ZStream.apply("file1", "file2", "file3", "file4", "file5")
  val reader = new Reader.UnitReader[Any, Any, String](stream)
  val writer1 = new Writer.UnitZWriter[Any, Any, String, Unit](
    s => s.tap(i => printLine("InsertWithDlq: " + i) *> ZIO.succeed((1 to 1000000000).foreach(_ => ()))).map(_=>())
  )

  val writer10 = new Writer.UnitZWriter[Any, Any, String, String](
    s => s.tap(i => printLine("Read: " + i))
  )
  val writer11 = new Writer.UnitZWriter[Any, Any, String, Unit](
    s => s.tap(i => printLine("Insert: " + i)).map(_=>())
  )
  val writer12 = new Writer.UnitZWriter[Any, Any, Unit, Unit](
    s => s.tap(_ => printLine("Dlq ")).map(_=>())
  )
  val writer2 = new Writer.UnitZWriter[Any, Any, String, Unit](
    s => s.tap(i => printLine("Counting: " + i)).map(_=>())
  )

  val c2: Writer[Any with Scope, Any, String, String] = (writer10 ++ (writer11 --> writer12)).map(_._1)
  val cL2: Writer[Any with Scope, Any, String, Unit] = c2 --> writer2
  val cross:       Writer[Any with Scope, Any, String, Any] = writer1 <*> writer2
  val crossLeft:   Writer[Any with Scope, Any, String, Unit] = writer1 <* writer2
  val crossRight:  Writer[Any with Scope, Any, String, Unit] = writer1 *> writer2
  val interleave: Writer[Any with Scope, Any, String, Any] = writer1 >>> writer2
  val merge:       Writer[Any with Scope, Any, String, Any] = writer1 <+> writer2
  val mergeLeft:   Writer[Any with Scope, Any, String, Unit] = writer1 <+ writer2
  val mergeRight:  Writer[Any with Scope, Any, String, Unit] = writer1 +> writer2
  val zip:         Writer[Any with Scope, Any, String, (Unit, Unit)] = writer1 ++ writer2

  def runPipe[B:ClassTag](name:String, w: Writer[Any with Scope, Any, String, B]): ZIO[Any, Throwable, Chunk[B]] = for {
    _ <- printLine(name)
    results <- ZIO.scoped((reader --> w).runCollect)
    _ <- results.mapZIO(r => printLine(r))
  } yield results

  val crossExpected = Chunk(
      (11,21),
      (11,22),
      (11,23),
      (11,24),
      (11,25),
      (11,26),
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
  val interleaveExpected = Chunk(
    11,
    21,
    12,
    22,
    13,
    23,
    14,
    24,
    15,
    25,
    16,
    26,
  )
  val mergeExpected = Chunk(
    11,
    21,
    12,
    22,
    13,
    23,
    14,
    24,
    15,
    25,
    16,
    26,
  )
  val mergeLeftExpected = Chunk(
    11,
    12,
    13,
    14,
    15,
    16,
  )
  val mergeRightExpected = Chunk(
    21,
    22,
    23,
    24,
    25,
    26,
  )
  val zipExpected = Chunk(
    (11,21),
    (12,22),
    (13,23),
    (14,24),
    (15,25),
    (16,26),
  )

  override def spec: Spec[Unit with TestEnvironment with Scope, Any] =
    suite("PipelineTest")(
      test("Simple pipeline with PipelineBuilder"){
        for {
//          crossResults <- runPipe("cross", cross)
//          crossLeftResults <- runPipe("crossLeft", crossLeft)
          crossLeft2Results <- runPipe("crossLeft2", cL2)
//          crossRightResults <- runPipe("crossRight", crossRight)
//          interleaveResults <- runPipe("interleave", interleave)
//          mergeResults <- runPipe("merge", merge)
//          mergeLeftResults <- runPipe("mergeLeft", mergeLeft)
//          mergeRightResults <- runPipe("mergeRight", mergeRight)
          zipResults <- runPipe("zip", zip)
        } yield {
//          assertTrue(crossResults.equals(crossExpected))
//          assertTrue(crossLeftResults.equals(crossLeftExpected))
          assertTrue(crossLeft2Results.equals(crossLeftExpected))
//          assertTrue(crossRightResults.equals(crossRightExpected))
//          assertTrue(interleaveResults.equals(interleaveExpected))
//          assertTrue(mergeResults.equals(mergeExpected))
//          assertTrue(mergeLeftResults.equals(mergeLeftExpected))
//          assertTrue(mergeRightResults.equals(mergeRightExpected))
          assertTrue(zipResults.equals(zipExpected))
          assertTrue(true)
        }
      },
    )

  override def bootstrap: ZLayer[Any, Throwable, Unit] = ZLayer.succeed()
}
