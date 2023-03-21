package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.configs.MultiClickhouseConfig
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.connect
import com.libertexgroup.configs.ClickhouseConfig
import com.libertexgroup.models.clickhouse.ClickhouseModel
import zio.{Scope, ZIO, ZLayer}
import zio.stream.{ZSink, ZStream}

import scala.util.{Failure, Success, Try}

protected[writers] class DefaultWriter[E] extends ClickhouseWriter[E, E with Scope with MultiClickhouseConfig, ClickhouseModel] {

  def write(stream: ZStream[E, Throwable, ClickhouseModel], config: ClickhouseConfig): ZIO[E with Scope, Throwable, Unit] = for {
    _ <- stream
      .groupedWithin(config.batchSize, config.syncDuration)
      .mapZIO(batch => for {
        error <- ZIO.scoped( for {
          conn <- connect
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
        } yield error ).provideSomeLayer(ZLayer.succeed(config))
      } yield error )
      .runScoped(ZSink.drain)
  } yield ()


  override def apply(stream: ZStream[E, Throwable, ClickhouseModel]):
                      ZIO[Scope with MultiClickhouseConfig with E, Throwable, Unit] = for {
      config <- ZIO.service[MultiClickhouseConfig]
      _ <- ZIO.foreachPar(config.chConfigs)(config => write(stream, config))
    } yield ()
}
