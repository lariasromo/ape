package com.libertexgroup.ape.writers.jdbc

import com.libertexgroup.ape.utils.GenericJDBCUtils.connect
import com.libertexgroup.configs.JDBCConfig
import com.libertexgroup.models.JDBCModel
import zio.stream.ZStream
import zio.{Scope, ZIO}

import scala.util.{Failure, Success, Try}

protected[writers] class DefaultWriter[E] extends JDBCWriter[E, E with Scope with JDBCConfig, JDBCModel] {
  override def apply(stream: ZStream[E, Throwable, JDBCModel]):
                      ZIO[Scope with JDBCConfig with E, Throwable, Unit] = for {
      config <- ZIO.service[JDBCConfig]
      _ <- stream
        .groupedWithin(config.batchSize, config.syncDuration)
        .mapZIO(batch => for {
          conn <- connect
          error <- ZIO.scoped(for {
            error <- ZIO.fromEither {
              Try {
                val sql = batch.head.sql
                val stmt = conn.prepareStatement(sql)
                batch.foreach(row => {
                  row.prepare(stmt)
                  stmt.addBatch()
                })
                stmt.executeBatch()
              } match {
                case Failure(exception) => {
                  exception.printStackTrace()
                  Left(exception)
                }
                case Success(value) => Right(value)
              }
            }
          } yield error )
        } yield error )
        .runDrain
    } yield ()
}
