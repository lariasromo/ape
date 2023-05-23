package ape.s3.readers

import ape.s3.configs.S3Config
import ape.s3.models.{CompressionType, dummy}
import ape.s3.utils.MinioContainer.MinioContainer
import ape.s3.utils.MinioContainerService
import ape.s3.utils.MinioContainerService.sampleRecords
import zio.s3.S3
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object S3JsonLinesCirceReaderTest extends ZIOSpec[S3 with S3Config with MinioContainer] {
  val location = "json"
  val s3Stream = ape.s3.Readers.fileReader[S3Config].fileReaderSimple(location).readFiles.jsonLinesCirce[dummy].stream

  override def spec: Spec[S3 with S3Config with MinioContainer with TestEnvironment with Scope, Any] =
    suite("S3JsonLinesCirceReaderTest")(
      test("Reads a json file") {
        for {
          d <- s3Stream.runCollect
          stream1 <- d.tapEach(s => s._2.runCollect).mapZIO(_._2.runCollect)
          data = stream1.flatten
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.size > 1)
          assertTrue(data.equals(sampleRecords))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Throwable, S3 with MinioContainer with S3Config] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
      ZLayer.fromZIO(MinioContainerService.loadSampleData)
}
