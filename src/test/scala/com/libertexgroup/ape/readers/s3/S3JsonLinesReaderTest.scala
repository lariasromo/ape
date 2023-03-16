package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.readers.s3.S3JsonLinesCirceReaderTest.location
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.ape.writers.sampleRecords
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object S3JsonLinesReaderTest extends ZIOSpec[S3 with S3Config with MinioContainer with S3FileReaderService] {
  val reader = Pipeline.readers.s3JsonLinesReader[dummy]

  override def spec: Spec[S3 with S3Config with MinioContainer with S3FileReaderService with TestEnvironment with Scope, Any] =
    suite("S3JsonLinesReaderTest")(
      test("Reads a json file"){
        for {
          stream <- reader.apply
          d <- stream.runCollect
          stream1 <- d.tapEach(s => s._2.runCollect).mapZIO(_._2.runCollect)
          data = stream1.flatten
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.size > 1)
          assertTrue(data.equals(sampleRecords))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Any, S3 with S3Config with MinioContainer with S3FileReaderService] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some("json")) >+>
      ( ZLayer.fromZIO(MinioContainerService.loadSampleData) >>> S3FileReaderServiceStatic.live(location))
}
