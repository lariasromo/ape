package com.libertexgroup.ape.transformers

import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZLayer}

object DefaultTransformerTest extends ZIOSpec[Unit] {
  val input: Chunk[Int] = Chunk(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
  val output: Chunk[Int] = Chunk(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024)

  implicit val algebra: Int => Int = a => scala.math.pow(2, a).toInt

  override def spec: Spec[Unit with TestEnvironment with Scope, Any] = suite("DefaultTransformerTest")(
    test("Applies a transformer that transforms data using an implicit algebra"){
      for {
        out <- new DefaultTransformer().apply(ZStream.fromChunk(input)).runCollect
      } yield {
        println(out.mkString(", "))
        println(output.mkString(", "))
        assertTrue(out.equals(output))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, Unit] = ZLayer.succeed()
}
