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
  def live: ZIO[system.System, SecurityException, ULayer[Has[ClickhouseConfig]]] = for {
    conf <- make
  } yield ZLayer.succeed( conf )

  def make: ZIO[system.System, SecurityException, ClickhouseConfig] = for {
    batchSize <- system.env("BATCH_SIZE")
    host <- system.env("CLICKHOUSE_HOST")
    port <- system.env("CLICKHOUSE_PORT")
    databaseName <- system.env("CLICKHOUSE_DATABASE_NAME")
    username <- system.env("CLICKHOUSE_USERNAME")
    password <- system.env("CLICKHOUSE_PASSWORD")
  } yield ClickhouseConfig(
    batchSize = port.flatMap(p => Try(p.toInt).toOption).getOrElse(10000),
    host = host.getOrElse(""),
    port = port.flatMap(p => Try(p.toInt).toOption).getOrElse(8123),
    databaseName = databaseName.getOrElse(""),
    username = username.getOrElse(""),
    password = password.getOrElse("")
  )
}