package ape.jdbc.pipes

import ape.jdbc.configs.JDBCConfig
import ape.jdbc.models.JDBCModel
import ape.jdbc.utils.GenericJDBCUtils.runConnect
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO, ZLayer}

import java.sql.{Connection, Statement}
import scala.reflect.ClassTag

protected[jdbc] class DefaultPipe[ET,
  Config <: JDBCConfig :Tag,
  Model <: JDBCModel :ClassTag] extends JDBCPipe[Config, ET, Model, Chunk[Model]] {
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
        .grouped(config.batchSize * config.parallelism)
        .flatMap{ batch => {
          ZStream.fromIterable {
            {
              val batchSpliced: Chunk[Chunk[Model]] = batch.split(config.parallelism)
              val indexes: Seq[Int] = (1 to config.parallelism)
              batchSpliced zip indexes
            }
              .map { case (batch, i) => (i, batch.length, insertBatch(batch).provideSomeLayer(ZLayer.succeed(config))) }
          }.mapZIOParByKey(_._1) {
            case (ix: Int, size: Int, eff: ZIO[Nothing, Throwable, Chunk[Model]]) =>
              for {
                _ <- printLine(s"Inserting batch on parallel index $ix and a batch size of $size")
                eff <- ZIO.scoped(eff)
              } yield eff
          }
        }}
    } yield errors
}
