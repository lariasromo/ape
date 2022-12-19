package com.libertexgroup.algebras.writers.clickhouse

import com.clickhouse.jdbc.{ClickHouseConnection, ClickHouseDataSource}
import com.libertexgroup.configs.ClickhouseConfig
import zio.console.Console
import zio.stream.ZStream
import zio.{Chunk, Has, ZIO, ZManaged}

import java.sql.ResultSet
import java.util.Properties
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object JDBCUtils {
  case class ConnectionWithZStream(connection: ClickHouseConnection, ZStream: ZStream[Any, Throwable, ResultSet])

  def queryToStreamCloseable(query: String): ZIO[Has[ClickhouseConfig], Throwable, ZManaged[Any, Throwable, ConnectionWithZStream]]
  =  for {
    config <- ZIO.access[Has[ClickhouseConfig]](_.get)
  } yield ZManaged.make {
    val connection = getConnection(config.jdbcUrl, config.username, config.password)
    ZIO(ConnectionWithZStream(connection, toStream(connection.createStatement().executeQuery(query))))
  } { r => ZIO.effectTotal(r.connection.close()) }

  def toStream(resultSet: ResultSet): ZStream[Any, Throwable, ResultSet] =
    ZStream.fromIterator(
      new Iterator[ResultSet] {
        def hasNext = resultSet.next()
        def next() = resultSet
      }
    )

  def query2Chunk[T:ClassTag](query: String)(implicit row2Object: ResultSet => T): ZIO[Has[ClickhouseConfig], Throwable, Chunk[T]]
  = for {
    closeableConn <- queryToStreamCloseable(query)
    ds <- closeableConn.use(_.ZStream.map(row2Object).runCollect)
  } yield ds

  val connect: ZIO[Has[ClickhouseConfig], Nothing, ZManaged[Any, Throwable, ClickHouseConnection]] = for {
    config <- ZIO.access[Has[ClickhouseConfig]](_.get)
  } yield getConnectionResource( config.jdbcUrl, config.username, config.password )

  def getConnectionResource(DB_URL: String, USER: String, PASS: String): ZManaged[Any, Throwable, ClickHouseConnection] =
    ZManaged.make( ZIO(getConnection(DB_URL, USER, PASS)) )(c => ZIO.effectTotal(c.close()))


  def executeQuery(sql: String): ZIO[Console with Has[ClickhouseConfig], Throwable, Unit] = for {
    conRes <- connect
    _ <- conRes.use(conn => ZIO {
      val statement = conn.createStatement()
      Try(statement.executeUpdate(sql)) match {
        case Failure(exception) => {
          exception.printStackTrace()
          throw exception
        }
        case Success(value) => value
      }
      statement.close()
    })
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
