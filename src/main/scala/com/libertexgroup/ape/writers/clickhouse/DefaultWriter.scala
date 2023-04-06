package com.libertexgroup.ape.writers.clickhouse

import com.clickhouse.jdbc.ClickHouseConnection
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.runConnect
import com.libertexgroup.configs.{ClickhouseConfig, MultiClickhouseConfig}
import com.libertexgroup.models.clickhouse.ClickhouseModel
import zio.stream.ZStream
import zio.{Chunk, ZIO, ZLayer}

import java.sql.Statement
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

//protected[writers] class DefaultWriter[E] extends ClickhouseWriter[E, E with Scope with MultiClickhouseConfig, ClickhouseModel] {
// The result of this writer are the records that failed to be inserted
protected[clickhouse] class DefaultWriter[ET, T <:ClickhouseModel :ClassTag] extends ClickhouseWriter[MultiClickhouseConfig,
  ET, T, Chunk[(T, Int)]] {
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

  def insertBatch(batch: Chunk[T], config:ClickhouseConfig): ZIO[Any, Throwable, Chunk[(T, Int)]] = for {
    errors <- runConnect(insertRetrieveResults(batch)).provideSomeLayer(ZLayer.succeed(config))
  } yield errors

  override def apply(stream: ZStream[ET, Throwable, T]):
  ZIO[MultiClickhouseConfig, Nothing, ZStream[ET, Throwable, Chunk[(T, Int)]]] = for {
      config <- ZIO.service[MultiClickhouseConfig]
      r = stream
        .grouped(config.chConfigs.length * config.chConfigs.head.batchSize)
        .flatMap{ batch => {
          ZStream.fromIterable{
            config.chConfigs.zip(batch.split(config.chConfigs.length))
              .map { case (config, batch) => insertBatch(batch, config)}
          }.mapZIOPar(config.chConfigs.length)(x => ZIO.scoped(x))
        }}
    } yield r
}
