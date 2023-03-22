package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.configs.MultiClickhouseConfig
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.connect
import com.libertexgroup.configs.ClickhouseConfig
import com.libertexgroup.models.clickhouse.ClickhouseModel
import zio.Console.printLine
import zio.{Chunk, Scope, ZIO, ZLayer}
import zio.stream.{ZSink, ZStream}

import scala.util.{Failure, Success, Try}

protected[writers] class DefaultWriter[E] extends ClickhouseWriter[E, E with Scope with MultiClickhouseConfig, ClickhouseModel] {
  def insertBatch(batch: Chunk[ClickhouseModel], config:ClickhouseConfig) = for {
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
  } yield error

  override def apply(stream: ZStream[E, Throwable, ClickhouseModel]): ZIO[E with Scope with MultiClickhouseConfig, Throwable, Unit]
  = for {
      config <- ZIO.service[MultiClickhouseConfig]
      _ <- stream
        .grouped(config.chConfigs.length * config.chConfigs.head.batchSize)
        .flatMap{ batch => {
          ZStream.fromIterable{
            config.chConfigs.zip(batch.split(config.chConfigs.length))
              .map { case (config, batch) => insertBatch(batch, config)}
          }.mapZIOPar(config.chConfigs.length)(x => ZIO.scoped(x))
        }}.runScoped(ZSink.drain)
    } yield ()
}
