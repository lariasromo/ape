package com.libertexgroup.configs

import zio.System.envOrElse
import zio.{ZIO, ZLayer}

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
  def live: ZLayer[System, SecurityException, JDBCConfig] = ZLayer(make)

  def make: ZIO[System, SecurityException, JDBCConfig] = for {
    host <- envOrElse("JDBC_HOST", "")
    port <- envOrElse("JDBC_PORT", "8123")
    databaseName <- envOrElse("JDBC_DATABASE_NAME", "")
    username <- envOrElse("JDBC_USERNAME", "")
    password <- envOrElse("JDBC_PASSWORD", "")
  } yield JDBCConfig(
    host = host,
    port = Try(port.toInt).toOption.getOrElse(8123),
    databaseName = databaseName,
    username = username,
    password = password
  )
}