package com.libertexgroup.ape.pipelines

import com.libertexgroup.ape.{Reader, Writer}
import zio.Console.printLine
import zio.stream.{ZSink, ZStream}
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZIO, ZLayer}

import scala.reflect.ClassTag

object WriterOpsTest extends ZIOSpec[Unit] {
  val stream = ZStream.apply(1, 2, 3, 4, 5, 6)
  val reader = new Reader.UnitReader[Any, Any, Int](stream)
  val writer1 = new Writer.UnitZWriter[Any, Any, Int, Int](s => s.tap(i => printLine("Writer1: " + i.toString)).map(_+10))
  val writer2 = new Writer.UnitZWriter[Any, Any, Int, Int](s => s.tap(i => printLine("Writer2: " + i.toString)).map(_+20))

  val cross: Writer[Any with Scope, Any, Int, Any] = writer1 <*> writer2
  val crossLeft: Writer[Any with Scope, Any, Int, Int] = writer1 <* writer2
  val crossRight: Writer[Any with Scope, Any, Int, Int] = writer1 *> writer2
  val intersperse: Writer[Any with Scope, Any, Int, Any] = writer1 >>> writer2
  val merge: Writer[Any with Scope, Any, Int, Any] = writer1 <+> writer2
  val mergeLeft: Writer[Any with Scope, Any, Int, Int] = writer1 <+ writer2
  val mergeRight: Writer[Any with Scope, Any, Int, Int] = writer1 +> writer2
  val zip: Writer[Any with Scope, Any, Int, (Int, Int)] = writer1 ++ writer2

  def runPipe[B:ClassTag](name:String, w: Writer[Any with Scope, Any, Int, B]): ZIO[Any, Throwable, Chunk[B]] = for {
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
  val intersperseExpected = Chunk(
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
          crossResults <- runPipe("cross", cross)
          crossLeftResults <- runPipe("crossLeft", crossLeft)
          crossRightResults <- runPipe("crossRight", crossRight)
          intersperseResults <- runPipe("intersperse", intersperse)
          mergeResults <- runPipe("merge", merge)
          mergeLeftResults <- runPipe("mergeLeft", mergeLeft)
          mergeRightResults <- runPipe("mergeRight", mergeRight)
          zipResults <- runPipe("zip", zip)
        } yield {
          assertTrue(crossResults.equals(crossExpected))
          assertTrue(crossLeftResults.equals(crossLeftExpected))
          assertTrue(crossRightResults.equals(crossRightExpected))
          assertTrue(intersperseResults.equals(intersperseExpected))
          assertTrue(mergeResults.equals(mergeExpected))
          assertTrue(mergeLeftResults.equals(mergeLeftExpected))
          assertTrue(mergeRightResults.equals(mergeRightExpected))
          assertTrue(zipResults.equals(zipExpected))
          assertTrue(true)
        }
      },
    )

  override def bootstrap: ZLayer[Any, Throwable, Unit] = ZLayer.succeed()
}
