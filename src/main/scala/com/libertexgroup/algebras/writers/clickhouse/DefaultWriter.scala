package com.libertexgroup.algebras.writers.clickhouse
import com.libertexgroup.algebras.writers.clickhouse.JDBCUtils.connect
import com.libertexgroup.configs.ClickhouseConfig
import com.libertexgroup.models.ClickhouseModel
import zio.stream.ZStream
import zio.{Has, ZIO}

import scala.util.{Failure, Success, Try}

class DefaultWriter[E] extends ClickhouseWriter[E] {
  val sql: String = "insert into foo(val1, val2) values(?, ?);"
  type EnvType = E with Has[ClickhouseConfig]

//  implicit val t: Array[Byte] => ClickhouseModel

  override def apply(stream: ZStream[E, Throwable, ClickhouseModel]): ZIO[E with Has[ClickhouseConfig], Throwable, Unit] =
    for {
      config <- ZIO.access[Has[ClickhouseConfig]](_.get)
      _ <- stream
        .grouped(config.batchSize)
        .map(batch => for {
          connect <- connect
          error <- connect.use(conn => for {
            error <- ZIO.fromEither {
              Try {
                val stmt = conn.prepareStatement(sql)
                batch.foreach(row => {
                  row.prepare(stmt)
                  stmt.addBatch()
                })
                stmt.executeUpdate()
              } match {
                case Failure(exception) => Left(exception)
                case Success(value) => Right(value)
              }
            }
          } yield error )
        } yield error )
        .runHead
    } yield ()

}
