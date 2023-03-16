package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object S3ParquetReaderTest extends ZIOSpec[S3 with MinioContainer with S3Config with S3FileReaderService] {
  val location = "parquet"
  val reader = Pipeline.readers.s3ParquetReader
  override def spec: Spec[S3 with MinioContainer with S3Config with S3FileReaderService with TestEnvironment with Scope, Any] =
    suite("S3ReaderTest")(
      test("Reads a parquet file"){
        for {
          stream <- reader.apply
          data <- stream.runCollect
          firstRecord = data.headOption
        } yield {
          assertTrue(firstRecord.isDefined)
          assertTrue(firstRecord.map(_.get("column0").toString).contains("Some string"))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Throwable, S3 with MinioContainer with S3Config with S3FileReaderService] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
      ( ZLayer.fromZIO(MinioContainerService.loadSampleData) >>> S3FileReaderServiceStatic.live(location))
}
