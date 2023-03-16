package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.readers.s3.{S3FileReaderService, S3FileReaderServiceStatic}
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.ape.utils.MinioContainerService.createBBucket
import com.libertexgroup.ape.writers.{sampleData, sampleRecords}
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object S3BytesWriterTest extends ZIOSpec[S3 with MinioContainer with S3Config with S3FileReaderService] {
  val location = "bytes"
  val writer = Pipeline.writers.s3AvroWriter[dummy]
  val reader = Pipeline.readers.s3AvroReader[dummy]

  override def spec: Spec[S3 with MinioContainer with S3Config with S3FileReaderService with TestEnvironment with Scope, Any] =
    suite("S3BytesWriterTest")(
      test("Writes entities to bytes"){
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
