package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object S3ParquetReaderTest extends ZIOSpec[S3 with MinioContainer with S3Config] {
  val reader = Pipeline.readers.s3ParquetReader
  override def spec: Spec[S3 with MinioContainer with S3Config with TestEnvironment with Scope, Any] = suite("S3ReaderTest")(
    test("Reads a parquet file"){
      for {
        _ <- MinioContainerService.loadSampleData
        stream <- reader.apply
        data <- stream.runCollect
        firstRecord = data.headOption
      } yield {
        assertTrue(firstRecord.isDefined)
        assertTrue(firstRecord.map(_.get("column0").toString).contains("Some string"))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Throwable, S3 with MinioContainer with S3Config] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some("parquet"))
}
