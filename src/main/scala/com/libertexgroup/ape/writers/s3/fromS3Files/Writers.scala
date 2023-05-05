package com.libertexgroup.ape.writers.s3.fromS3Files

import com.libertexgroup.ape.readers.s3.S3FileReader
import com.libertexgroup.configs.{RedisConfig, S3Config}
import zio.Tag

class Writers[ SConfig <: S3Config :Tag ](reader: S3FileReader[SConfig]) {
  def default = new noneBacked.Writers[SConfig](reader)
  def withRedis[RConfig <: RedisConfig :Tag] = new redisBacked.Writers[SConfig, RConfig](reader)
  def withQueue = new zioBacked.Writers[SConfig](reader)
}
