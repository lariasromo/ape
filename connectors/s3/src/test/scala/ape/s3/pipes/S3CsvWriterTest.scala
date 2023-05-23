package ape.s3.pipes

import ape.s3.configs.S3Config
import ape.s3.models.{CompressionType, dummy}
import ape.s3.utils.MinioContainer.MinioContainer
import ape.s3.utils.MinioContainerService
import ape.s3.utils.MinioContainerService.setup
import zio.s3.S3
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZLayer}

object S3CsvWriterTest extends ZIOSpec[S3 with MinioContainer with S3Config] {
  val location = "bytes"
  val sampleDataWithCharacters = ZStream.fromChunk(Chunk(
    dummy(",\",,", "£$%^&"),
    dummy(",\",,", "'1,'1'"),
  ))

  override def spec: Spec[S3 with MinioContainer with S3Config with TestEnvironment with Scope, Any] =
    suite("S3CsvWriterTest")(
      test("Writes entities to csv strings"){
        for {
          files <- ape.s3.Readers.fileReader[S3Config].fileReaderSimple(location).readFiles.text.stream.runCollect
          data <- ZStream.fromChunk(files).flatMap(_._2).runCollect
        } yield {
          assertTrue(data.nonEmpty)
          val csvRecords = Chunk(
            "£$%^&;\",\"\",,\"",
            "\"'1,'1'\";\",\"\",,\""
          )
          assertTrue(data.equals(csvRecords))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Any, S3 with MinioContainer with S3Config] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
      setup(
        ape.s3.Pipes
          .fromData[S3Config].text
          .csv[dummy]( ";", Some(Seq("b", "a")))
          .write(sampleDataWithCharacters)
      )
}
