package com.libertexgroup.pipes.redis.generalPurpose

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Pipes[Config <: RedisConfig : Tag] {
  // general purpose
  def avro[ZE, T: ClassTag : Tag : SchemaFor : Encoder](queueName: String): Pipe[Config, ZE, T, T] = new AvroPipe[ZE, Config, T](queueName)

  def default[ZE, T: ClassTag : Tag : SchemaFor : Encoder](queueName: String): Pipe[Config, ZE, T, T] = avro[ZE, T](queueName)

  def string[ZE](queueName: String): Pipe[Config, ZE, String, String] = new StringPipe[ZE, Config](queueName)
}