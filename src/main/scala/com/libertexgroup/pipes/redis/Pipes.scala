package com.libertexgroup.pipes.redis

import com.libertexgroup.pipes.redis
import com.libertexgroup.configs.RedisConfig
import zio.Tag

class Pipes[Config <: RedisConfig : Tag]{
  def backPressure = new redis.backPressure.Pipes[Config]
  def generalPurpose = new redis.generalPurpose.Pipes[Config]
}