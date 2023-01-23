package com.libertexgroup.configs

import zio.duration.{Duration, durationInt}
import zio.{Has, ULayer, ZIO, ZLayer, system}

import scala.util.Try

case class ClickhouseConfig(
                             batchSize: Int,
                             syncDuration: Duration,
                             host: String,
                             port: Int,
                             databaseName: String,
                             username: String,
                             password: String
                           ){
  val jdbcUrl = s"jdbc:clickhouse://$host:$port/$databaseName"
}
//                           ) extends JDBCConfig

object ClickhouseConfig {
  def live: ZLayer[system.System, SecurityException, Has[ClickhouseConfig]] = ZLayer.fromEffect(make)

  def make: ZIO[system.System, SecurityException, ClickhouseConfig] = for {
    syncDuration <- system.envOrElse("CLICKHOUSE_SYNC_DURATION", "5")
    batchSize <- system.envOrElse("CLICKHOUSE_BATCH_SIZE", "10000")
    host <- system.envOrElse("CLICKHOUSE_HOST", "")
    port <- system.envOrElse("CLICKHOUSE_PORT", "8123")
    databaseName <- system.envOrElse("CLICKHOUSE_DATABASE_NAME", "")
    username <- system.envOrElse("CLICKHOUSE_USERNAME", "")
    password <- system.envOrElse("CLICKHOUSE_PASSWORD", "")
  } yield ClickhouseConfig(
    syncDuration = Try(syncDuration.toInt.minutes).toOption.getOrElse(5.minutes),
    batchSize = Try(batchSize.toInt).toOption.getOrElse(10000),
    host = host,
    port = Try(port.toInt).toOption.getOrElse(8123),
    databaseName = databaseName,
    username = username,
    password = password
  )
}