package com.libertexgroup.ape.writers.jdbc

import com.libertexgroup.ape.utils.GenericJDBCUtils.runConnect
import com.libertexgroup.configs.JDBCConfig
import com.libertexgroup.models.jdbc.JDBCModel
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO, ZLayer}

import java.sql.{Connection, Statement}
import scala.reflect.ClassTag

protected[jdbc] class DefaultWriter[ET,
  Config <: JDBCConfig :Tag,
  Model <: JDBCModel :ClassTag] extends JDBCWriter[Config, ET, Model, Chunk[Model]] {
  def insertRetrieveErrors(batch: Chunk[Model]): Connection => Chunk[Model] = conn => {
    val sql = batch.head.sql
    val stmt = conn.prepareStatement(sql)
    batch.foreach(row => {
      row.prepare(stmt)
      stmt.addBatch()
    })
    batch.zip(stmt.executeBatch())
      .filter { case(_, result) => result.equals(Statement.EXECUTE_FAILED) }
      .map { case(row, _) => row }
  }

  def insertBatch(batch: Chunk[Model]): ZIO[Config, Throwable, Chunk[Model]] = for {
    errors <- runConnect(insertRetrieveErrors(batch))
  } yield errors

  override protected[this] def pipe(i: ZStream[ET, Throwable, Model]):
    ZIO[Config, Nothing, ZStream[ET, Throwable, Chunk[Model]]] =
    for {
      config <- ZIO.service[Config]
      errors = i
        .groupedWithin(config.batchSize, config.syncDuration)
        .mapZIO(batch => for {
          error <- ZIO.scoped(for {
            error <- insertBatch(batch).provideSomeLayer(ZLayer.succeed(config))
          } yield error )
        } yield error )
    } yield errors
}
