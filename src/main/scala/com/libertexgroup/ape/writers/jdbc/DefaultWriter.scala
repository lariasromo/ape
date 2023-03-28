package com.libertexgroup.ape.writers.jdbc

import com.libertexgroup.ape.utils.GenericJDBCUtils.runConnect
import com.libertexgroup.configs.JDBCConfig
import com.libertexgroup.models.jdbc.JDBCModel
import zio.stream.ZStream
import zio.{Chunk, ZIO, ZLayer}

import java.sql.{Connection, Statement}

protected[writers] class DefaultWriter[ET] extends JDBCWriter[JDBCConfig, ET, JDBCModel, Chunk[JDBCModel]] {
  def insertRetrieveErrors(batch: Chunk[JDBCModel]): Connection => Chunk[JDBCModel] = conn => {
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

  def insertBatch(batch: Chunk[JDBCModel], config:JDBCConfig): ZIO[Any, Throwable, Chunk[JDBCModel]] = for {
    errors <- runConnect(insertRetrieveErrors(batch)).provideSomeLayer(ZLayer.succeed(config))
  } yield errors

  override def apply(stream: ZStream[ET, Throwable, JDBCModel]): ZIO[JDBCConfig, Nothing, ZStream[ET, Throwable, Chunk[JDBCModel]]]
  =
    for {
      config <- ZIO.service[JDBCConfig]
      errors = stream
        .groupedWithin(config.batchSize, config.syncDuration)
        .mapZIO(batch => for {
          error <- ZIO.scoped(for {
            error <- insertBatch(batch, config)
          } yield error )
        } yield error )
    } yield errors

}
