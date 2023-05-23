package ape.redis

import ape.reader.Reader
import ape.redis.configs.RedisConfig
import ape.redis.readers.{AvroReader, RedisReaders, StringReader}
import ape.utils.Utils.:=
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag


class Readers[Config <: RedisConfig :Tag] extends RedisReaders[Config]{
  def avro[T :ClassTag :SchemaFor : Decoder : Encoder](queueName:String, limit:Int= -1):
    Reader[Config, Any, T] = new AvroReader[Any, T, Config](queueName, limit)
  def default[T>:Null: ClassTag :SchemaFor : Decoder : Encoder](queueName:String, limit:Int= -1):
    Reader[Config, Any, T] = avro(queueName, limit)
  def string(queueName:String, limit:Int= -1): Reader[Config, Any, String] =
    new StringReader[Any, Config](queueName, limit)
}

object Readers {
  def readers[Config <: RedisConfig :Tag](implicit d1: Config := RedisConfig) = new Readers[Config]()
}
