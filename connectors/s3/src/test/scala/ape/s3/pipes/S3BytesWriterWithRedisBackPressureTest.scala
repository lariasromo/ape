package ape.s3.pipes

import ape.redis.configs.RedisConfig
import ape.redis.utils.RedisContainerService
import ape.s3.models.{CompressionType, S3ConfigTest, dummy}
import ape.s3.utils.MinioContainer.MinioContainer
import ape.s3.utils.MinioContainerService
import ape.s3.utils.MinioContainerService.{sampleData, sampleRecords, setup}
import com.redis.testcontainers.RedisContainer
import zio.s3.S3
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object S3BytesWriterWithRedisBackPressureTest extends ZIOSpec[S3 with MinioContainer with S3ConfigTest with RedisContainer with RedisConfig] {
  val location = "bytes"

  val reader = ape.s3.Readers.fileReader[S3ConfigTest]
    .fileReaderSimple(location)
    .readFilesWithRedis[RedisConfig]
    .avro[dummy]

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
      setup(ape.s3.Pipes.fromData[S3ConfigTest].encoded.avro[dummy].write(sampleData)) ++
        RedisContainerService.live
}
