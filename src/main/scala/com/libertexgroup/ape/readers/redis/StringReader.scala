package com.libertexgroup.ape.readers.redis

import com.libertexgroup.configs.RedisConfig
import org.redisson.api.RQueue
import org.redisson.client.codec.StringCodec
import zio.stream.ZStream
import zio.{Chunk, Schedule, Tag, ZIO, durationInt}

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

class StringReader[ZE, Config <: RedisConfig :Tag](queueName:String, limit:Int = -1)
  extends RedisReader[Config, ZE, String]{
  def readRedis: ZIO[Config, Nothing, ZStream[Any, Throwable, String]] = for {
    config <- ZIO.service[Config]
  } yield ZStream
    .fromZIO(
      ZIO.fromTry{
        Try(
          {
            val q: RQueue[String] = config.redisson.getQueue[String](queueName, new StringCodec())
            val e: Chunk[String] = if(limit >= 0) Chunk.fromIterable(q.poll(limit).asScala) else Chunk(q.poll)
            e
          }
        )
      })
    .flatMap(ZStream.fromChunk(_))
    .filter(_!=null)

  override protected[this] def read: ZIO[Config, Throwable, ZStream[ZE, Throwable, String]] =
    for {
      redis <- readRedis
    } yield if(limit >= 0) redis else redis.retry(Schedule.spaced(1.second)).forever

}
