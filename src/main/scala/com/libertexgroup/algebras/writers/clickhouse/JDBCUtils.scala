package com.libertexgroup.algebras.writers.clickhouse

import com.clickhouse.jdbc.{ClickHouseConnection, ClickHouseDataSource}
import com.libertexgroup.configs.ClickhouseConfig
import zio.stream.ZStream
import zio.{Chunk, Scope, ZIO}

import java.sql.ResultSet
import java.util.Properties
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object JDBCUtils {
  case class ConnectionWithZStream(connection: ClickHouseConnection, ZStream: ZStream[Any, Throwable, ResultSet])

  def queryToStreamCloseable(query: String): ZIO[ClickhouseConfig with Any with Scope, Nothing, ConnectionWithZStream]
  =  ZIO.acquireRelease {
    for {
      config <- ZIO.service[ClickhouseConfig]
    } yield {
      val connection = getConnection(config.jdbcUrl, config.username, config.password)
      ConnectionWithZStream(connection, toStream(connection.createStatement().executeQuery(query)))
    }
  } { r => ZIO.succeed(r.connection.close()) }

  def toStream(resultSet: ResultSet): ZStream[Any, Throwable, ResultSet] =
    ZStream.fromIterator(
      new Iterator[ResultSet] {
        def hasNext = resultSet.next()
        def next() = resultSet
      }
    )

  def query2Chunk[T:ClassTag](query: String)(implicit row2Object: ResultSet => T): ZIO[ClickhouseConfig with Any with Scope, Throwable, Chunk[T]]
  = for {
    closeableConn <- queryToStreamCloseable(query)
    ds <- ZIO.scoped {
      closeableConn.ZStream.map(row2Object).runCollect
    }
  } yield ds

  val connect: ZIO[ClickhouseConfig with Any with Scope, Nothing, ClickHouseConnection] =  ZIO
    .acquireRelease( for {
    config <- ZIO.service[ClickhouseConfig]
  } yield getConnection(config.jdbcUrl, config.username, config.password)
  )(c => ZIO.succeed(c.close()))

  def getConnectionResource(DB_URL: String, USER: String, PASS: String): ZIO[Any with Scope, Nothing, ClickHouseConnection] =
    ZIO.acquireRelease( ZIO.succeed(getConnection(DB_URL, USER, PASS)) )(c => ZIO.succeed(c.close()))


  def executeQuery(sql: String): ZIO[ClickhouseConfig with Any with Scope, Nothing, Unit] = for {
    conRes <- connect
    _ <- ZIO.scoped {
      ZIO.succeed {
        val statement = conRes.createStatement()
        Try(statement.executeUpdate(sql)) match {
          case Failure(exception) => {
            exception.printStackTrace()
            throw exception
          }
          case Success(value) => value
        }
        statement.close()
      }
    }
  } yield ()

  def getConnection(DB_URL: String, USER: String, PASS: String): ClickHouseConnection = {
    try {
      val properties = new Properties()
      properties.setProperty("socket_timeout", "3000000")
      properties.setProperty("user", USER)
      properties.setProperty("password", PASS)
      val dataSource = new ClickHouseDataSource(DB_URL, properties)
      dataSource.getConnection()
    } catch {
      case e: Exception => {
        e.printStackTrace()
        null
      }
    }
  }
}
