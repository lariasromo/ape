package ape.redis.pipes.generalPurpose

import ape.pipe.Pipe
import ape.redis.configs.RedisConfig
import org.redisson.api.RQueue
import org.redisson.client.codec.StringCodec
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.util.Try

class StringPipe[ZE, Config<:RedisConfig :Tag](queueName:String) extends Pipe[Config, ZE, String, String]{

  override protected[this] def pipe(i: ZStream[ZE, Throwable, String]): ZIO[Config, Throwable, ZStream[ZE, Throwable, String]] =
    for {
      config <- ZIO.service[Config]
    } yield i.tap(r => ZIO.fromTry{
      Try {
        val q: RQueue[String] = config.redisson.getQueue[String](queueName, new StringCodec())
        q.add(r)
      }
    })
}
