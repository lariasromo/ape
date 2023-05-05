package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.writers.s3.fromS3Files._
import com.libertexgroup.ape.{Ape, Reader}
import com.libertexgroup.configs.{RedisConfig, S3Config}
import zio.Tag
import zio.s3.S3ObjectSummary


abstract class S3FileReader[Config <: S3Config :Tag] extends Reader[Config, Any, S3ObjectSummary]{

  def readFiles: noneBacked.Writers[Config] = Ape.writers.s3FileReader[Config](this).default

  def readFilesWithRedis[RConfig <: RedisConfig :Tag]:
  redisBacked.Writers[Config, RConfig] = Ape.writers.s3FileReader[Config](this).withRedis[RConfig]

  def readFilesWithQueue: zioBacked.Writers[Config] = Ape.writers.s3FileReader[Config](this).withQueue

}
