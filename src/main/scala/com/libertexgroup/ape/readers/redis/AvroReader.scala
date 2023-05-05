package com.libertexgroup.ape.readers.redis

import com.libertexgroup.ape.utils.AvroUtils.implicits._
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.redisson.api.RQueue
import org.redisson.client.codec.ByteArrayCodec
import zio.stream.ZStream
import zio.{Chunk, Schedule, Tag, ZIO, durationInt}

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.reflect.ClassTag
import scala.util.Try

class AvroReader[ZE,T : ClassTag :SchemaFor : Decoder : Encoder,Config <: RedisConfig :Tag]
(queueName:String, limit:Int= -1) extends RedisReader[Config, ZE, T]{

  def readRedis: ZIO[Config, Nothing, ZStream[Any, Throwable, T]] = for {
    config <- ZIO.service[Config]
  } yield ZStream
    .fromZIO(
      ZIO.fromTry{
        Try {
          val q: RQueue[Array[Byte]] = config.redisson.getQueue[Array[Byte]](queueName, new ByteArrayCodec())
          val bs: Chunk[Array[Byte]] = if(limit >= 0) Chunk.fromIterable(q.poll(limit).asScala) else Chunk(q.poll)
//          bs.toList.decode[T]
          bs.filter(_!=null).map(l => {
            Try(l.decode[T]().headOption).toOption.flatten
          })
        }
      })
    .flatMap(ZStream.fromChunk(_))

    .filter(_.isDefined).map(_.get)
    .filter(_!=null)

  override protected[this] def read: ZIO[Config, Throwable, ZStream[ZE, Throwable, T]] =
    for {
      redis <- readRedis
    } yield if(limit >= 0) redis else redis.retry(Schedule.spaced(1.second)).forever

}
