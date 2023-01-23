package com.libertexgroup.configs

import zio.duration.{Duration, durationInt}
import zio.{Has, ULayer, ZIO, ZLayer, system}

import scala.util.Try

case class JDBCConfig(
                             host: String,
                             port: Int,
                             databaseName: String,
                             username: String,
                             password: String
                           ){
  val jdbcUrl = s"jdbc:clickhouse://$host:$port/$databaseName"
}

object JDBCConfig {
  def live: ZLayer[system.System, SecurityException, Has[JDBCConfig]] = ZLayer.fromEffect(make)

  def make: ZIO[system.System, SecurityException, JDBCConfig] = for {
    host <- system.envOrElse("JDBC_HOST", "")
    port <- system.envOrElse("JDBC_PORT", "8123")
    databaseName <- system.envOrElse("JDBC_DATABASE_NAME", "")
    username <- system.envOrElse("JDBC_USERNAME", "")
    password <- system.envOrElse("JDBC_PASSWORD", "")
  } yield JDBCConfig(
    host = host,
    port = Try(port.toInt).toOption.getOrElse(8123),
    databaseName = databaseName,
    username = username,
    password = password
  )
}