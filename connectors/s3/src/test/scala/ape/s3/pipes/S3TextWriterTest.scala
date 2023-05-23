package ape.s3.pipes

import ape.s3.configs.S3Config
import ape.s3.models.CompressionType
import ape.s3.utils.MinioContainer.MinioContainer
import ape.s3.utils.MinioContainerService
import ape.s3.utils.MinioContainerService.setup
import zio.s3.S3
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Chunk, Scope, ZLayer}

object S3TextWriterTest  extends ZIOSpec[S3 with MinioContainer with S3Config] {
  val sampleStrings: Chunk[String] = Chunk(
    "string 1",
    "other string",
    "lorem ipsum"
  )

  val data: ZStream[Any, Nothing, String] = ZStream.fromChunk(sampleStrings)
  val location = "bytes"

  override def spec: Spec[S3 with MinioContainer with S3Config with TestEnvironment with Scope, Any] =
    suite("S3TextWriterTest")(
      test("Writes strings to S3"){
        for {
          files <- ape.s3.Readers.fileReader[S3Config].fileReaderSimple(location).readFiles.text.stream.runCollect
          data <- ZStream.fromChunk(files).flatMap(_._2).runCollect
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.equals(sampleStrings))
        }
      },
    )


  override def bootstrap: ZLayer[Any, Any, S3 with MinioContainer with S3Config] =
      MinioContainerService.s3Layer >+>
        MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
        setup(ape.s3.Pipes.fromData[S3Config].text.default.runDrain(data))
}