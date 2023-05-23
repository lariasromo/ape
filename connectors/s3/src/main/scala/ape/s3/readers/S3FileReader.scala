package ape.s3.readers

import ape.reader.Reader
import ape.redis.configs.RedisConfig
import ape.s3.Readers
import ape.s3.configs.S3Config
import ape.s3.pipes.fromS3Files.{noneBacked, redisBacked, zioBacked}
import zio.Tag
import zio.s3.S3ObjectSummary


abstract class S3FileReader[Config <: S3Config :Tag] extends Reader[Config, Any, S3ObjectSummary]{

  def readFiles: noneBacked.Pipes[Config] = Readers.contentReader[Config](this).default

  def readFilesWithRedis[RConfig <: RedisConfig :Tag]: redisBacked.Pipes[Config, RConfig] =
    Readers.contentReader[Config](this).withRedis[RConfig]

  def readFilesWithQueue: zioBacked.Pipes[Config] = Readers.contentReader[Config](this).withQueue

}
