package ape.s3.pipes.fromS3Files

import ape.redis.configs.RedisConfig
import ape.s3.configs.S3Config
import ape.s3.readers.S3FileReader
import ape.utils.Utils.:=
import zio.Tag

class Pipes[ SConfig <: S3Config :Tag ](reader: S3FileReader[SConfig]) {
  def default = new noneBacked.Pipes[SConfig](reader)
  def withRedis[RConfig <: RedisConfig :Tag](implicit d1: RConfig := RedisConfig) =
    new redisBacked.Pipes[SConfig, RConfig](reader)
  def withQueue = new zioBacked.Pipes[SConfig](reader)
}
