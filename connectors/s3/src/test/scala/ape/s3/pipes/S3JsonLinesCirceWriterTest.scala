package ape.s3.pipes

import ape.s3.configs.S3Config
import ape.s3.models.{CompressionType, dummy}
import ape.s3.utils.MinioContainer.MinioContainer
import ape.s3.utils.MinioContainerService
import ape.s3.utils.MinioContainerService.{sampleData, sampleRecords, setup}
import zio.s3.S3
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object S3JsonLinesCirceWriterTest extends ZIOSpec[S3 with MinioContainer with S3Config] {
  val location = "json"

  override def spec: Spec[S3 with MinioContainer with S3Config with TestEnvironment with Scope, Any] =
    suite("S3JsonLinesCirceWriterTest")(
      test("Writes strings to S3"){
        for {
          files <- ape.s3.Readers.fileReader[S3Config].fileReaderSimple(location).readFiles.jsonLinesCirce[dummy].stream.runCollect
          data <- ZStream.fromChunk(files).flatMap(_._2).runCollect
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.equals(sampleRecords))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Any, S3 with MinioContainer with S3Config] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
      setup(ape.s3.Pipes.fromData[S3Config].jsonLines.circe[dummy].write(sampleData))
}