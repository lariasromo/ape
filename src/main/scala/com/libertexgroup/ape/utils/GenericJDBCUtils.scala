package com.libertexgroup.ape.utils

import com.libertexgroup.configs.JDBCConfig
import zio.stream.ZStream
import zio.{Chunk, Scope, ZIO}

import java.sql.{Connection, DriverManager, ResultSet}
import java.util.Properties
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object GenericJDBCUtils {
  case class ConnectionWithZStream(connection: Connection, ZStream: ZStream[Any, Throwable, ResultSet])

  def query2Chunk[T: ClassTag](query: String)
                              (implicit row2Object: ResultSet => T): ZIO[JDBCConfig, Nothing, Chunk[T]] =
    ZIO.scoped {
      for {
        conn <- connect
      } yield toChunk(conn.createStatement().executeQuery(query))
    }

  def query2Stream[T: ClassTag](query: String)
                               (implicit row2Object: ResultSet => T): ZIO[JDBCConfig, Nothing, ZStream[Any,
    Throwable,
    T]] =
    ZIO.scoped {
      for {
        conn <- connect
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

  val connect: ZIO[JDBCConfig with Scope, Nothing, Connection] = ZIO
    .acquireRelease(for {
      config <- ZIO.service[JDBCConfig]
    } yield getConnection(config.driverName, config.jdbcUrl, config.username, config.password)
    )(c => ZIO.succeed(c.close()))


  def runConnect[T](effect: Connection => T): ZIO[JDBCConfig, Throwable, T] = for {
    errors <- ZIO.scoped( for {
      conn <- connect
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
    } yield errors )
  } yield errors

  def executeQuery(sql: String): ZIO[JDBCConfig with Scope, Nothing, Unit] = for {
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

  def getConnection(DRIVER_NAME: String, DB_URL: String, USER: String, PASS: String): Connection = {
    try {
      val properties = new Properties()
      properties.setProperty("socket_timeout", "3000000")
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
