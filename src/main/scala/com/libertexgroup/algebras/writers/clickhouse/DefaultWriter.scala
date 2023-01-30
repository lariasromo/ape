package com.libertexgroup.algebras.writers.clickhouse
import com.libertexgroup.algebras.writers.clickhouse.JDBCUtils.connect
import com.libertexgroup.configs.ClickhouseConfig
import com.libertexgroup.models.ClickhouseModel
import zio.{Scope, ZIO}
import zio.stream.ZStream

import scala.util.{Failure, Success, Try}

class DefaultWriter[E] extends ClickhouseWriter[E, E with Scope with ClickhouseConfig, ClickhouseModel] {
  override def apply(stream: ZStream[E, Throwable, ClickhouseModel]): ZIO[ClickhouseConfig with Scope with E, Throwable, Unit] =
    for {
      config <- ZIO.service[ClickhouseConfig]
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
