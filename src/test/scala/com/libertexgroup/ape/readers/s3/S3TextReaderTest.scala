package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.{Scope, ZLayer}
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}

object S3TextReaderTest extends ZIOSpec[S3 with S3Config with MinioContainer] {
  val reader = Pipeline.readers.s3TextReader
  override def spec: Spec[S3 with S3Config with MinioContainer with TestEnvironment with Scope, Any] = suite("S3ReaderTest")(
    test("Reads a text file"){
      for {
        _ <- MinioContainerService.loadSampleData
        stream <- reader.apply
        data <- stream.runCollect
        firstRecord = data.headOption
      } yield {
        assertTrue(firstRecord.isDefined)
        assertTrue(firstRecord.exists(_.contains("Some string")))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, S3 with S3Config with MinioContainer] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some("plaintext"))
}
