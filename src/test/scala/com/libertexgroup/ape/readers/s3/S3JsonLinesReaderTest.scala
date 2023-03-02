package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.ape.writers.sampleRecords
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object S3JsonLinesReaderTest extends ZIOSpec[S3 with S3Config with MinioContainer] {
  val reader = Pipeline.readers.s3JsonLinesReader[dummy]("json")
  override def spec: Spec[S3 with S3Config with MinioContainer with TestEnvironment with Scope, Any] = suite("S3JsonLinesReaderTest")(
    test("Reads a json file"){
      for {
        _ <- MinioContainerService.loadSampleData
        stream <- reader.apply
        data <- stream.runCollect
      } yield {
        assertTrue(data.nonEmpty)
        assertTrue(data.size > 1)
        assertTrue(data.equals(sampleRecords))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, S3 with S3Config with MinioContainer] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some("json"))
}
