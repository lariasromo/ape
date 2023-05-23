package ape.redis.configs

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import zio.System.env
import zio.{Duration, ZIO, ZLayer, durationInt}

case class RedisConfig(config:Config) {
  lazy val redisson: RedissonClient = Redisson.create(config)
}

object RedisConfig {
  def makeSingleServerFromEnv(prefix:Option[String]=None): ZIO[Any, SecurityException, RedisConfig] = for {
    host <- env(prefix.map(s => s + "_").getOrElse("") + "REDIS_HOST")
    port <- env(prefix.map(s => s + "_").getOrElse("") + "REDIS_PORT")
    ssl <- env(prefix.map(s => s + "_").getOrElse("") + "REDIS_SSL")
    timeout <- env(prefix.map(s => s + "_").getOrElse("") + "REDIS_TIMEOUT")
  } yield makeSingleServer(
    host.getOrElse("127.0.0.1"),
    port.getOrElse("6379").toInt,
    Duration fromJava java.time.Duration.parse(timeout.getOrElse("PT10S")),
    ssl.exists(_.toBoolean)
  )

  def makeSingleServer(host:String, port:Int=6379, timeout:Duration=10.seconds, ssl:Boolean=false): RedisConfig = {
    val protocol = if (ssl) "rediss" else "redis"
    val address = s"$protocol://$host:$port"
    val config = new Config()
    config
      .useSingleServer
      .setAddress(address)
      .setTimeout(timeout.toMillis.toInt)
      .setConnectTimeout(timeout.toMillis.toInt)
    RedisConfig(config)
  }

  def liveSingleServer(
                        host:String,
                        port:Int=6379,
                        timeout:Duration=10.seconds,
                        ssl:Boolean=false
                      ): ZLayer[Any, SecurityException, RedisConfig] =
    ZLayer.succeed(makeSingleServer(host, port, timeout, ssl))

  def liveSingleServerFromEnv(prefix:Option[String]=None): ZLayer[Any, SecurityException, RedisConfig] =
    ZLayer.fromZIO(makeSingleServerFromEnv(prefix))
}
