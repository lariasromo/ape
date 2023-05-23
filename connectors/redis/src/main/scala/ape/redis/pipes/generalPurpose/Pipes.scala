package ape.redis.pipes.generalPurpose

import ape.pipe.Pipe
import ape.redis.configs.RedisConfig
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Pipes[ZE, Config <: RedisConfig : Tag] {
  // will try to encode case classes into bytes using avro so it can be easily decoded when read
  def typed[T: ClassTag : Tag : SchemaFor : Encoder](queueName: String): Pipe[Config, ZE, T, T] =
    new AvroPipe[ZE, Config, T](queueName)

  def string(queueName: String): Pipe[Config, ZE, String, String] = new StringPipe[ZE, Config](queueName)
}

