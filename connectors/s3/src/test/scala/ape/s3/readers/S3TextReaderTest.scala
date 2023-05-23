package ape.s3.readers

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.MinioContainer.MinioContainer
import ape.s3.utils.MinioContainerService
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object S3TextReaderTest extends ZIOSpec[S3 with S3Config with MinioContainer] {
  val location = "plaintext"
  val stream = ape.s3.Readers.fileReader[S3Config].fileReaderSimple(location).readFiles.text.stream

  override def spec: Spec[S3 with S3Config with MinioContainer with TestEnvironment with Scope, Any] =
    suite("S3ReaderTest")(
      test("Reads a text file") {
        for {
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

  override def bootstrap: ZLayer[Any, Any, S3 with S3Config with MinioContainer] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
      ZLayer.fromZIO(MinioContainerService.loadSampleData)
}
