package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.readers.s3.{S3FileReaderService, S3FileReaderServiceStatic}
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.ape.utils.MinioContainerService.createBBucket
import com.libertexgroup.ape.writers.s3.S3JsonLinesCirceWriterTest.location
import com.libertexgroup.ape.writers.{sampleData, sampleRecords}
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object S3JsonLinesWriterTest  extends ZIOSpec[S3 with MinioContainer with S3Config with S3FileReaderService] {
  val location = "json"
  val reader = Pipeline.readers.s3JsonLinesReader[dummy]
  val writer = Pipeline.writers.s3JsonLinesWriter[dummy]

  override def spec: Spec[S3 with MinioContainer with S3Config with S3FileReaderService with TestEnvironment with Scope, Any] =
    suite("S3JsonLinesWriterTest")(
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

  override def bootstrap: ZLayer[Any, Any, S3 with MinioContainer with S3Config with S3FileReaderService] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
      S3FileReaderServiceStatic.live(location)
}