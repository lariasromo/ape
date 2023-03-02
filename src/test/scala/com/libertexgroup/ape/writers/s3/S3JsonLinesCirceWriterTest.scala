package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.ape.utils.MinioContainerService.createBBucket
import com.libertexgroup.ape.writers.{sampleData, sampleRecords}
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZLayer}

object S3JsonLinesCirceWriterTest extends ZIOSpec[S3 with MinioContainer with S3Config] {
  val location = "json"
  val reader = Pipeline.readers.s3JsonLinesCirceReader[dummy](location)
  val writer = Pipeline.writers.s3JsonLinesCirceWriter[dummy]

  override def spec: Spec[S3 with MinioContainer with S3Config with TestEnvironment with Scope, Any] = suite("S3JsonLinesCirceWriterTest")(
    test("Writes strings to S3"){
      for {
        _ <- createBBucket
        _ <- writer.apply(sampleData)
        stream <- reader.apply
        data <- stream.runCollect
      } yield {
        assertTrue(data.nonEmpty)
        assertTrue(data.equals(sampleRecords))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, S3 with MinioContainer with S3Config] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location))
}