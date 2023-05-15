package com.libertexgroup.configs

import zio.System.envOrElse
import zio.{Duration, ZIO, ZLayer, durationInt}

import scala.util.Try

case class JDBCConfig(
                       batchSize: Int,
                       syncDuration: Duration,
                       driverName:String,
                       jdbcUrl: String,
                       username: String,
                       password: String,
                       socketTimeout: Duration = 3.minutes
                       )

object JDBCConfig {
  def live(prefix:Option[String]=None): ZLayer[Any, SecurityException, JDBCConfig] = ZLayer.fromZIO(make(prefix))

  def make(prefix:Option[String]=None): ZIO[Any, SecurityException, JDBCConfig] = for {
    syncDuration <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_SYNC_DURATION", "5")
    batchSize <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CLICKHOUSE_BATCH_SIZE", "10000")
    driverName <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "JDBC_DRIVER_NAME", "")
    jdbcUrl <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "JDBC_URL", "")
    username <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "JDBC_USERNAME", "")
    password <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "JDBC_PASSWORD", "")
    socketTimeout <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "JDBC_SOCKET_TIMEOUT", "")
  } yield JDBCConfig(
    syncDuration = Try(syncDuration.toInt.minutes).toOption.getOrElse(5.minutes),
    batchSize = Try(batchSize.toInt).toOption.getOrElse(10000),
    driverName = driverName,
    jdbcUrl = jdbcUrl,
    username = username,
    password = password,
    socketTimeout = Try(socketTimeout.toInt.minutes).toOption.getOrElse(3.minutes)
  )
}