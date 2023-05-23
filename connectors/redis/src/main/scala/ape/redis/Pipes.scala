package ape.redis

import ape.redis.configs.RedisConfig
import ape.utils.Utils.:=
import zio.Tag

class Pipes[Config <: RedisConfig : Tag]{
  def backPressure = new ape.redis.pipes.backPressure.Pipes[Config]
  def generalPurpose[ZE](implicit d1: ZE := Any, d2: Config := RedisConfig) = new ape.redis.pipes.generalPurpose.Pipes[ZE, Config]
}

object Pipes {
  def pipes[Config <:RedisConfig :Tag](implicit d: Config := RedisConfig) = new Pipes[Config]
}