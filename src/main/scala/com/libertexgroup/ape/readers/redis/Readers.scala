package com.libertexgroup.ape.readers.redis

import com.libertexgroup.ape.Reader
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Readers[Config <: RedisConfig :Tag] {
  def avro[ZE, T :ClassTag :SchemaFor : Decoder : Encoder](queueName:String, limit:Int= -1):
    Reader[Config, ZE, T] = new AvroReader[ZE, T, Config](queueName, limit)
  def default[ZE, T>:Null: ClassTag :SchemaFor : Decoder : Encoder](queueName:String, limit:Int= -1):
    Reader[Config, ZE, T] = avro(queueName, limit)
  def string[ZE](queueName:String, limit:Int= -1): Reader[Config, ZE, String] =
    new StringReader[ZE, Config](queueName, limit)
}
