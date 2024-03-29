package ape.clickhouse.utils

import ape.clickhouse.configs.{ClickhouseConfig, MultiClickhouseConfig}
import ape.clickhouse.models.ClickhouseLookupModel
import com.clickhouse.jdbc.{ClickHouseConnection, ClickHouseDataSource, ClickHouseStatement}
import zio.stream.ZStream
import zio.{Chunk, Duration, Scope, ZIO, ZLayer}

import java.sql.ResultSet
import java.util.Properties
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object ClickhouseJDBCUtils {
  case class ConnectionWithZStream(connection: ClickHouseConnection, ZStream: ZStream[Any, Throwable, ResultSet])

  def getChunk[T: ClassTag](eff: ClickHouseConnection => ResultSet)
                           (implicit dec: ResultSet => T ): ZIO[ClickhouseConfig, Nothing, Chunk[T]] =
    ZIO.scoped {
      for {
        conf <- ZIO.service[ClickhouseConfig]
        conn <- connect
        chk <- ZIO.fromTry(
          Try {
            toChunk { eff(conn) }
          })
          .catchAll(ex => for {
            _ <- ZIO.logError(ex.getMessage)
            _ <- ZIO.logError("No data found on node: " + conf)
          } yield Chunk.empty)
      } yield chk
  }

  def lookupModel2Chunk[
    Model <: ClickhouseLookupModel[T],
    T: ClassTag
  ](model: Model): ZIO[ClickhouseConfig, Throwable, Chunk[T]] = {
    implicit val decoder: ResultSet => T = model.lookupDecode
    getChunk(conn => {
      val st = conn.prepareStatement(model.lookupQuery)
      model.lookupBind(st)
      st.executeQuery()
    }  )
  }

  def query2Chunk[T: ClassTag](query: String)
                              (implicit row2Object: ResultSet => T): ZIO[ClickhouseConfig, Throwable, Chunk[T]] =
    getChunk(conn => conn.createStatement().executeQuery(query))

  def query2ChunkMulti[T: ClassTag](query: String)
                                   (implicit row2Object: ResultSet => T): ZIO[MultiClickhouseConfig, Throwable, Chunk[T]] = {
    for {
      confs <- ZIO.service[MultiClickhouseConfig]
      chks <- ZIO.foreachPar(confs.chConfigs)(conf => query2Chunk(query).provideSomeLayer(ZLayer.succeed(conf)))
    } yield Chunk.fromIterable(chks).flatten
  }

  def runConnect[T](effect: ClickHouseConnection => T): ZIO[ClickhouseConfig, Throwable, T] = for {
    errors <- ZIO.scoped(for {
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
    } yield errors)
  } yield errors

  val connect: ZIO[ClickhouseConfig with Scope, Nothing, ClickHouseConnection] = ZIO
    .acquireRelease(for {
      config <- ZIO.service[ClickhouseConfig]
    } yield getConnection(config.jdbcUrl, config.username, config.password, config.socketTimeout)
    )(c => ZIO.succeed(c.close()))

  def queryIterator[T](resultSet: ResultSet)(implicit row2Object: ResultSet => T): Iterator[T] = {
    new Iterator[T] {
      def hasNext: Boolean = resultSet.next()

      def next(): T = row2Object(resultSet)
    }
  }

  def toChunk[T: ClassTag](resultSet: ResultSet)(implicit row2Object: ResultSet => T): Chunk[T] =
    Chunk.fromIterator(queryIterator(resultSet))

  def executeQuery(sql: String): ZIO[ClickhouseConfig with Scope, Nothing, Unit] = for {
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

  def executeQueryMulti(sql: String): ZIO[MultiClickhouseConfig, Nothing, Unit] =
    ZIO.service[MultiClickhouseConfig].flatMap(config =>
      ZIO.foreach(config.chConfigs)(config =>
        ZIO.scoped(executeQuery(sql)).provideSomeLayer(ZLayer.succeed(config))
      ) *> ZIO.unit
    )

  def getConnection(DB_URL: String, USER: String, PASS: String, TIMEOUT: Duration): ClickHouseConnection = {
    try {
      val properties = new Properties()
      properties.setProperty("socket_timeout", TIMEOUT.toMillis.toString)
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
