package ape.clickhouse.pipes

import ape.clickhouse.configs.{ClickhouseConfig, MultiClickhouseConfig}
import ape.clickhouse.models.ClickhouseModel
import ape.clickhouse.utils.ClickhouseJDBCUtils.runConnect
import com.clickhouse.jdbc.ClickHouseConnection
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO, ZLayer}

import java.sql.Statement
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

//protected[writers] class DefaultWriter[E] extends ClickhouseWriter[E, E with Scope with MultiClickhouseConfig, ClickhouseModel] {
// The result of this writer are the records that failed to be inserted
protected[clickhouse] class DefaultPipe[ET, T <:ClickhouseModel :ClassTag, Config <: MultiClickhouseConfig :Tag]
  extends ClickhousePipe[Config, ET, T, Chunk[(T, Int)]] {
  def insertRetrieveResults(batch: Chunk[T]): ClickHouseConnection => Chunk[(T, Int)] = conn => {
    val stmt = conn.prepareStatement(batch.head.sql)
    batch.foreach(row => {
      row.prepare(stmt)
      stmt.addBatch()
    })
    val tryEx: Chunk[Int] = Try(stmt.executeBatch()) match {
      case Failure(exception) => {
        println(exception.getMessage)
        // if the whole batch failed, mark all rows as failed
        batch.map(_ => Statement.EXECUTE_FAILED)
      }
      case Success(value) => Chunk.fromArray(value)
    }

    batch.zip(tryEx)
  }

  def insertBatch(batch: Chunk[T]): ZIO[ClickhouseConfig, Nothing, Chunk[(T, Int)]] = for {
    errors <- runConnect(insertRetrieveResults(batch))
      .catchAll{ error => {
          for {
            _ <- printLine(error.getMessage).catchAll(_=>ZIO.unit)
          } yield batch.map(t => (t, Statement.EXECUTE_FAILED)) // if the whole batch failed, mark all rows as failed
        }
      }
  } yield errors

  override protected[this] def pipe(i: ZStream[ET, Throwable, T]):
    ZIO[Config, Throwable, ZStream[ET, Throwable, Chunk[(T, Int)]]] = for {
    config <- ZIO.service[Config]
    r = i
      .grouped(config.chConfigs.length * config.chConfigs.head.batchSize)
      .flatMap{ batch => {
        ZStream.fromIterable{
          config.chConfigs.zip(batch.split(config.chConfigs.length))
            .map { case (config, batch) => insertBatch(batch).provideSomeLayer(ZLayer.succeed(config)) }
        }.mapZIOPar(config.chConfigs.length)(x => ZIO.scoped(x))
      }}
  } yield r
}
