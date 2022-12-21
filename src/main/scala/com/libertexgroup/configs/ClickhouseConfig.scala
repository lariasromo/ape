package com.libertexgroup.configs

import zio.{Has, ULayer, ZIO, ZLayer, system}

import scala.util.Try

case class ClickhouseConfig(
                             batchSize: Int,
                             host: String,
                             port: Int,
                             databaseName: String,
                             username: String,
                             password: String
                           ){
  val jdbcUrl = s"jdbc:clickhouse://$host:$port/$databaseName"
}

object ClickhouseConfig {
  def live: ZLayer[system.System, SecurityException, Has[ClickhouseConfig]] = ZLayer.fromEffect(make)

  def make: ZIO[system.System, SecurityException, ClickhouseConfig] = for {
    batchSize <- system.envOrElse("BATCH_SIZE", "10000")
    host <- system.envOrElse("CLICKHOUSE_HOST", "")
    port <- system.envOrElse("CLICKHOUSE_PORT", "8123")
    databaseName <- system.envOrElse("CLICKHOUSE_DATABASE_NAME", "")
    username <- system.envOrElse("CLICKHOUSE_USERNAME", "")
    password <- system.envOrElse("CLICKHOUSE_PASSWORD", "")
  } yield ClickhouseConfig(
    batchSize = Try(port.toInt).toOption.getOrElse(10000),
    host = host,
    port = Try(port.toInt).toOption.getOrElse(8123),
    databaseName = databaseName,
    username = username,
    password = password
  )
}