package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.readers.s3.TextReader
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.ape.utils.MinioContainerService.createBBucket
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZLayer}

object S3TextWriterTest  extends ZIOSpec[S3 with MinioContainer with S3Config] {
  val sampleStrings: Chunk[String] = Chunk(
    "string 1",
    "other string",
    "lorem ipsum"
  )

  val data: ZStream[Any, Nothing, String] = ZStream.fromChunk(sampleStrings)

  override def spec: Spec[S3 with MinioContainer with S3Config with TestEnvironment with Scope, Any] = suite("S3TextWriterTest")(
    test("Writes strings to S3"){
      for {
        _ <- createBBucket
        _ <- new TextWriter().apply(data)
        stream <- new TextReader().apply
        data <- stream.runCollect
      } yield {
        assertTrue(data.nonEmpty)
        assertTrue(data.equals(sampleStrings))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, S3 with MinioContainer with S3Config] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some("bytes"))
}