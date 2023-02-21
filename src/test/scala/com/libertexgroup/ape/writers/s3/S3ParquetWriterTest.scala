package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.readers.s3.{AvroReader, ParquetReader}
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.ape.utils.MinioContainerService.createBBucket
import com.libertexgroup.ape.writers.s3.S3BytesWriterTest.{suite, test}
import com.libertexgroup.ape.writers.{sampleData, sampleRecords}
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.{Scope, ZLayer, durationInt}
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}

object S3ParquetWriterTest extends ZIOSpec[S3 with MinioContainer with S3Config] {

  override def spec: Spec[S3 with MinioContainer with S3Config with TestEnvironment with Scope, Any] = suite("S3ParquetWriterTest")(
    test("Writes entities to parquet"){
      for {
        _ <- createBBucket
        _ <- new ParquetWriter[Any, dummy](10, 1.minute).apply(sampleData)
        stream <- new ParquetReader[dummy]().apply
        data <- stream.runCollect
      } yield {
        assertTrue(data.nonEmpty)
        assertTrue(data.equals(sampleRecords))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, S3 with MinioContainer with S3Config] =
    MinioContainerService.s3Layer >+>
      MinioContainerService.configLayer(CompressionType.NONE, Some("parquet"))
}
