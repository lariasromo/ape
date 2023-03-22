package com.libertexgroup.configs

import zio.{Duration, ULayer, ZIO, ZLayer, durationInt}
import zio.System.{env, envOrElse}

import scala.util.Try

case class ClickhouseConfig(
                             batchSize: Int,
                             syncDuration: Duration,
                             host: String,
                             port: Int,
                             databaseName: String,
                             username: String,
                             password: String,
                             clusterName: Option[String]
                           ){
  val jdbcUrl = s"jdbc:clickhouse://$host:$port/$databaseName"
}

object ClickhouseConfig {
  def live: ZLayer[Any, SecurityException, ClickhouseConfig] = ZLayer(make)

  def make: ZIO[Any, SecurityException, ClickhouseConfig] = for {
    clusterName <- env("CLICKHOUSE_CLUSTER_NAME")
    syncDuration <- envOrElse("CLICKHOUSE_SYNC_DURATION", "5")
    batchSize <- envOrElse("CLICKHOUSE_BATCH_SIZE", "10000")
    host <- envOrElse("CLICKHOUSE_HOST", "")
    port <- envOrElse("CLICKHOUSE_PORT", "8123")
    databaseName <- envOrElse("CLICKHOUSE_DATABASE_NAME", "")
    username <- envOrElse("CLICKHOUSE_USERNAME", "")
    password <- envOrElse("CLICKHOUSE_PASSWORD", "")
  } yield ClickhouseConfig(
    syncDuration = Try(syncDuration.toInt.minutes).toOption.getOrElse(5.minutes),
    batchSize = Try(batchSize.toInt).toOption.getOrElse(10000),
    host = host,
    port = Try(port.toInt).toOption.getOrElse(8123),
    databaseName = databaseName,
    username = username,
    password = password,
    clusterName = clusterName
  )
}