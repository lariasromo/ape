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
                       password: String
                       )

object JDBCConfig {
  def live: ZLayer[System, SecurityException, JDBCConfig] = ZLayer(make)

  def make: ZIO[System, SecurityException, JDBCConfig] = for {
    syncDuration <- envOrElse("CLICKHOUSE_SYNC_DURATION", "5")
    batchSize <- envOrElse("CLICKHOUSE_BATCH_SIZE", "10000")
    driverName <- envOrElse("JDBC_DRIVER_NAME", "")
    jdbcUrl <- envOrElse("JDBC_URL", "")
    username <- envOrElse("JDBC_USERNAME", "")
    password <- envOrElse("JDBC_PASSWORD", "")
  } yield JDBCConfig(
    syncDuration = Try(syncDuration.toInt.minutes).toOption.getOrElse(5.minutes),
    batchSize = Try(batchSize.toInt).toOption.getOrElse(10000),
    driverName = driverName,
    jdbcUrl = jdbcUrl,
    username = username,
    password = password
  )
}