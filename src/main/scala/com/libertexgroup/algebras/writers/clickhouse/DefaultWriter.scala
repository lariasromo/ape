package com.libertexgroup.algebras.writers.clickhouse
import com.libertexgroup.algebras.writers.clickhouse.JDBCUtils.connect
import com.libertexgroup.configs.ClickhouseConfig
import com.libertexgroup.models.ClickhouseModel
import zio.clock.Clock
import zio.stream.ZStream
import zio.{Has, ZIO}

import scala.util.{Failure, Success, Try}

class DefaultWriter[E] extends ClickhouseWriter[E, E with Clock with Has[ClickhouseConfig], ClickhouseModel] {
  override def apply(stream: ZStream[E, Throwable, ClickhouseModel]): ZIO[E with Clock with Has[ClickhouseConfig], Throwable, Unit] =
    for {
      config <- ZIO.access[Has[ClickhouseConfig]](_.get)
      _ <- stream
        .groupedWithin(config.batchSize, config.syncDuration)
        .mapM(batch => for {
          conn <- connect
          error <- conn.use(conn => for {
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
