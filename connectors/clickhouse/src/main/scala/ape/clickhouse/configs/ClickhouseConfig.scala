package ape.clickhouse.configs

import zio.System.{env, envOrElse}
import zio.{Duration, ZIO, ZLayer, durationInt}

import scala.util.Try

case class ClickhouseConfig(
                             batchSize: Int,
                             syncDuration: Duration,
                             host: String,
                             port: Int,
                             databaseName: String,
                             username: String,
                             password: String,
                             socketTimeout: Duration = 3.minutes
  ){
  val jdbcUrl = s"jdbc:clickhouse://$host:$port/$databaseName"
}

object ClickhouseConfig {
  def live(prefix:Option[String]=None): ZLayer[Any, SecurityException, ClickhouseConfig] = ZLayer(make(prefix))

  def make(prefix:Option[String]=None): ZIO[Any, SecurityException, ClickhouseConfig] = for {
    syncDuration <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_SYNC_DURATION", "5")
    batchSize <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_BATCH_SIZE", "10000")
    host <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_HOST", "")
    port <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_PORT", "8123")
    databaseName <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_DATABASE_NAME", "")
    username <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_USERNAME", "")
    password <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_PASSWORD", "")
    socketTimeout <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_SOCKET_TIMEOUT", "")
  } yield ClickhouseConfig(
    syncDuration = Try(syncDuration.toInt.minutes).toOption.getOrElse(5.minutes),
    batchSize = Try(batchSize.toInt).toOption.getOrElse(10000),
    host = host,
    port = Try(port.toInt).toOption.getOrElse(8123),
    databaseName = databaseName,
    username = username,
    password = password,
    socketTimeout = Try(socketTimeout.toInt.seconds).toOption.getOrElse(3.minutes),
  )
}