package ape.jdbc.utils

import ape.jdbc.configs.JDBCConfig
import zio.stream.ZStream
import zio.{Chunk, Duration, Scope, Tag, ZIO}

import java.sql.{Connection, DriverManager, ResultSet}
import java.util.Properties
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object GenericJDBCUtils {
  case class ConnectionWithZStream(connection: Connection, ZStream: ZStream[Any, Throwable, ResultSet])

  def query2Chunk[T: ClassTag, Config <: JDBCConfig : Tag](query: String)
                                                          (implicit row2Object: ResultSet => T): ZIO[Config, Nothing, Chunk[T]] =
    ZIO.scoped[Config] {
      for {
        conn <- connect[Config]
      } yield toChunk(conn.createStatement().executeQuery(query))
    }

  def query2Stream[T: ClassTag, Config <: JDBCConfig : Tag](query: String)
                                                           (implicit row2Object: ResultSet => T): ZIO[Config, Nothing, ZStream[Any,
    Throwable,
    T]] =
    ZIO.scoped[Config] {
      for {
        conn <- connect[Config]
      } yield toStream(conn.createStatement().executeQuery(query))
    }

  def queryIterator[T](resultSet: ResultSet)(implicit row2Object: ResultSet => T): Iterator[T] = {
    new Iterator[T] {
      def hasNext: Boolean = resultSet.next()

      def next(): T = row2Object(resultSet)
    }
  }

  def toChunk[T: ClassTag](resultSet: ResultSet)(implicit row2Object: ResultSet => T): Chunk[T] =
    Chunk.fromIterator(queryIterator(resultSet))

  def toStream[T: ClassTag](resultSet: ResultSet)(implicit row2Object: ResultSet => T): ZStream[Any, Throwable, T] =
    ZStream.fromIterator(queryIterator(resultSet))

  def connect[Config <: JDBCConfig : Tag]: ZIO[Config with Scope, Nothing, Connection] =
    ZIO.acquireRelease(for {
      config <- ZIO.service[Config]
    } yield getConnection(config.driverName, config.jdbcUrl, config.username, config.password, config.socketTimeout)
    )(c => ZIO.succeed(c.close()))


  def runConnect[T, Config <: JDBCConfig : Tag](effect: Connection => T): ZIO[Config, Throwable, T] = for {
    errors <- ZIO.scoped[Config] {
      for {
        conn <- connect[Config]
        errors <- ZIO.fromEither {
          Try {
            effect(conn)
          } match {
            case Failure(exception) => {
              exception.printStackTrace()
              Left(exception)
            }
            case Success(value) => Right(value)
          }
        }
      } yield errors
    }
  } yield errors

  def executeQuery[Config <: JDBCConfig : Tag](sql: String): ZIO[Config with Scope, Nothing, Unit] = for {
    conRes <- connect[Config]
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

  def getConnection(DRIVER_NAME: String, DB_URL: String, USER: String, PASS: String, TIMEOUT: Duration): Connection = {
    try {
      val properties = new Properties()
      properties.setProperty("socket_timeout", TIMEOUT.toMillis.toString)
      properties.setProperty("user", USER)
      properties.setProperty("password", PASS)
      Class.forName(DRIVER_NAME)
      DriverManager.getConnection(DB_URL, properties)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        null
      }
    }
  }
}
