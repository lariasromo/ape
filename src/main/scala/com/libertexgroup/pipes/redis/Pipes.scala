package com.libertexgroup.pipes.redis

import com.libertexgroup.pipes.redis
import com.libertexgroup.configs.RedisConfig
import com.libertexgroup.utils.Utils.:=
import zio.Tag

class Pipes[Config <: RedisConfig : Tag]{
  def backPressure = new redis.backPressure.Pipes[Config]
  def generalPurpose[ZE](implicit d1: ZE := Any, d2: Config := RedisConfig) = new redis.generalPurpose.Pipes[ZE, Config]
}