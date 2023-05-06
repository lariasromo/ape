package com.libertexgroup.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.{RedisConfig, S3Config}
import com.libertexgroup.pipes.s3.fromS3Files.zioBacked.Pipes
import com.libertexgroup.pipes.s3.fromS3Files.{noneBacked, redisBacked}
import zio.Tag
import zio.s3.S3ObjectSummary


abstract class S3FileReader[Config <: S3Config :Tag] extends Reader[Config, Any, S3ObjectSummary]{

  def readFiles: noneBacked.Pipes[Config] = Ape.pipes.s3FileReader[Config](this).default

  def readFilesWithRedis[RConfig <: RedisConfig :Tag]:
  redisBacked.Pipes[Config, RConfig] = Ape.pipes.s3FileReader[Config](this).withRedis[RConfig]

  def readFilesWithQueue: Pipes[Config] = Ape.pipes.s3FileReader[Config](this).withQueue

}
