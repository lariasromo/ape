package com.libertexgroup.ape.utils

import com.clickhouse.jdbc.{ClickHouseConnection, ClickHouseDataSource}
import com.libertexgroup.configs.ClickhouseConfig
import zio.stream.ZStream
import zio.{Chunk, Scope, ZIO}

import java.sql.ResultSet
import java.util.Properties
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object ClickhouseJDBCUtils {
  case class ConnectionWithZStream(connection: ClickHouseConnection, ZStream: ZStream[Any, Throwable, ResultSet])

  def query2Chunk[T: ClassTag](query: String)
                              (implicit row2Object: ResultSet => T): ZIO[ClickhouseConfig, Nothing, Chunk[T]] =
    ZIO.scoped {
      for {
        conn <- connect
      } yield toChunk(conn.createStatement().executeQuery(query))
    }

  def queryIterator[T](resultSet: ResultSet)(implicit row2Object: ResultSet => T): Iterator[T] = {
    new Iterator[T] {
      def hasNext: Boolean = resultSet.next()
      def next(): T = row2Object(resultSet)
    }
  }

  def toChunk[T: ClassTag](resultSet: ResultSet)(implicit row2Object: ResultSet => T): Chunk[T] =
    Chunk.fromIterator(queryIterator(resultSet))

  val connect: ZIO[ClickhouseConfig with Any with Scope, Nothing, ClickHouseConnection] = ZIO
    .acquireRelease(for {
      config <- ZIO.service[ClickhouseConfig]
    } yield getConnection(config.jdbcUrl, config.username, config.password)
    )(c => ZIO.succeed(c.close()))


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
