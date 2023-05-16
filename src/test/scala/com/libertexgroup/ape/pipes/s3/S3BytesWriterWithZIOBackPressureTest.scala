package com.libertexgroup.ape.pipes.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.models.{S3ConfigTest, dummy}
import com.libertexgroup.ape.utils.MinioContainer.MinioContainer
import com.libertexgroup.ape.utils.MinioContainerService.setup
import com.libertexgroup.ape.utils.{MinioContainerService, RedisContainerService}
import com.libertexgroup.ape.pipes.{sampleData, sampleRecords}
import com.libertexgroup.configs.RedisConfig
import com.libertexgroup.models.s3.CompressionType
import com.libertexgroup.pipes.s3.fromS3Files.S3WithBackPressure
import com.redis.testcontainers.RedisContainer
import zio.s3.S3
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object S3BytesWriterWithZIOBackPressureTest extends ZIOSpec[S3 with MinioContainer with S3ConfigTest with RedisContainer with RedisConfig] {
  val location = "bytes"

  val reader = Ape.readers.s3[S3ConfigTest]
    .fileReaderSimple(location)
    .readFiles
    .avro[dummy]
    .map(S3WithBackPressure.zio[dummy].backPressure)

  override def spec: Spec[S3 with MinioContainer with S3ConfigTest with RedisContainer with RedisConfig with TestEnvironment with Scope, Any] =
    suite("S3BytesWriterTest")(
      test("Writes entities to bytes") {
        for {
          files <- reader.stream.runCollect
          data <- ZStream.fromChunk(files).flatMap(_._2).runCollect
        } yield {
          assertTrue(data.nonEmpty)
          assertTrue(data.equals(sampleRecords))
        }
      },
    )

  override def bootstrap: ZLayer[Any, Any, S3 with MinioContainer with S3ConfigTest with RedisContainer with RedisConfig] =
    MinioContainerService.s3Layer >+> MinioContainerService.configLayer(CompressionType.NONE, Some(location)) >+>
      setup(Ape.pipes.s3[S3ConfigTest].encoded.avro[dummy].write(sampleData)) ++
        RedisContainerService.live
}
