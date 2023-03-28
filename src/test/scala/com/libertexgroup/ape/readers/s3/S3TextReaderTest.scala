package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.CompressionType
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object S3TextReaderTest extends ZIOSpec[S3 with S3Config with MinioContainer with S3FileReaderService] {
  val location = "plaintext"
  val reader = Ape.readers.s3TextReader
  override def spec: Spec[S3 with S3Config with MinioContainer with S3FileReaderService with TestEnvironment with Scope, Any] = suite("S3ReaderTest")(
    test("Reads a text file"){
      for {
        stream <- reader.apply
        data <- stream.runCollect
        s <- ZIO.fromOption(data.headOption)
        stream1 <- s._2.runCollect
        firstRecord = stream1.headOption
      } yield {
        assertTrue(firstRecord.isDefined)
        assertTrue(firstRecord.exists(_.contains("Some string")))
      }
    },
  )

  override def bootstrap: ZLayer[Any, Any, S3 with S3Config with MinioContainer with S3FileReaderService] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
      ( ZLayer.fromZIO(MinioContainerService.loadSampleData) >>> S3FileReaderServiceStatic.live(location))
}
