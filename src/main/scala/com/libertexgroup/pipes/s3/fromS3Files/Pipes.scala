package com.libertexgroup.pipes.s3.fromS3Files

import com.libertexgroup.configs.{RedisConfig, S3Config}
import com.libertexgroup.readers.s3.S3FileReader
import com.libertexgroup.utils.Utils.:=
import zio.Tag

class Pipes[ SConfig <: S3Config :Tag ](reader: S3FileReader[SConfig]) {
  def default = new noneBacked.Pipes[SConfig](reader)
  def withRedis[RConfig <: RedisConfig :Tag](implicit d1: RConfig := RedisConfig) = new redisBacked.Pipes[SConfig, RConfig](reader)
  def withQueue = new zioBacked.Pipes[SConfig](reader)
}
