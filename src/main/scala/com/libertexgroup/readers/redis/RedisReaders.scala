package com.libertexgroup.readers.redis

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}

import scala.reflect.ClassTag


trait RedisReaders[Config <: RedisConfig] {
  def avro[T :ClassTag :SchemaFor : Decoder : Encoder](queueName:String, limit:Int= -1): Reader[Config, Any, T]
  def default[T>:Null: ClassTag :SchemaFor : Decoder : Encoder](queueName:String, limit:Int= -1): Reader[Config, Any, T]
  def string(queueName:String, limit:Int= -1): Reader[Config, Any, String]
}