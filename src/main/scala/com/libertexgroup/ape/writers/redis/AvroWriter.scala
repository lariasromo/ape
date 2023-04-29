package com.libertexgroup.ape.writers.redis

import com.libertexgroup.ape.Writer
import com.libertexgroup.ape.utils.AvroUtils.implicits._
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.redisson.api.RQueue
import org.redisson.client.codec.ByteArrayCodec
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag
import scala.util.Try

class AvroWriter[ZE, Config<:RedisConfig :Tag, T :ClassTag :SchemaFor : Encoder :Tag](queueName:String)
  extends Writer[Config, ZE, T, T]{
  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[Config, Throwable, ZStream[ZE, Throwable, T]] = {
    for {
      config <- ZIO.service[Config]
    } yield i.tap(r => ZIO.fromTry{
      Try {
        val q: RQueue[Array[Byte]] = config.redisson.getQueue[Array[Byte]](queueName, new ByteArrayCodec())
        val bytes = r.encode[T].orNull
        q.add(bytes)
      }
    })
  }
}
