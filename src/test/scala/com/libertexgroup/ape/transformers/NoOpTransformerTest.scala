package com.libertexgroup.ape.transformers

import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZLayer}

object NoOpTransformerTest extends ZIOSpec[Unit] {
  val input: Chunk[Int] = Chunk(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

  implicit val algebra: Int => Int = a => scala.math.pow(2, a).toInt

  override def spec: Spec[Unit with TestEnvironment with Scope, Any] = suite("DefaultTransformerTest")(
    test("Applies a transformer that transforms data using an implicit algebra"){
      for {
        out <- new NoOpTransformer().apply(ZStream.fromChunk(input)).runCollect
      } yield {
        println(out.mkString(", "))
        println(input.mkString(", "))
        assertTrue(out.equals(input))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, Unit] = ZLayer.succeed()
}

