package com.libertexgroup.pipes.s3.fromS3Files

import com.libertexgroup.configs.{RedisConfig, S3Config}
import com.libertexgroup.readers.s3.S3FileReader
import zio.Tag

class Pipes[ SConfig <: S3Config :Tag ](reader: S3FileReader[SConfig]) {
  def default = new noneBacked.Pipes[SConfig](reader)
  def withRedis[RConfig <: RedisConfig :Tag] = new redisBacked.Pipes[SConfig, RConfig](reader)
  def withQueue = new zioBacked.Pipes[SConfig](reader)
}
