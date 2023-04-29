package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService
import com.libertexgroup.ape.utils.MinioContainerService.setup
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.CompressionType
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
          files <- Ape.readers.s3[S3Config].fileReaderSimple(location).readFiles.text.stream.runCollect
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
      setup(Ape.writers.s3[S3Config].fromData
        .csv[Any, dummy]( ";", Some(Seq("b", "a")))
        .write(sampleDataWithCharacters))
}
