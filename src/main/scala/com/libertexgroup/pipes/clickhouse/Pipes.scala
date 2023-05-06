package com.libertexgroup.pipes.clickhouse

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs._
import com.libertexgroup.models.clickhouse.{ClickhouseDLQModel, ClickhouseModel}
import zio.stream.ZStream
import zio.{Chunk, Tag}

import java.sql.Statement
import scala.reflect.ClassTag

protected[pipes] class Pipes[Config <: MultiClickhouseConfig :Tag]() {

  def default[ET, T <:ClickhouseModel :ClassTag]: Pipe[Config, ET, T, Chunk[(T, Int)]] =
    new DefaultPipe[ET, T, Config]

  def writerWithDLQ[E, DLQ <: ClickhouseModel :ClassTag, T <:ClickhouseDLQModel[DLQ] :ClassTag]:
    Pipe[Config, E, T, Chunk[(DLQ, Int)]] = default[E, T]
    .map(_.filter(_._2.equals(Statement.EXECUTE_FAILED)).map(r => r._1.dlq))
    .mapZ(_.flatMap(dlq => ZStream.fromChunk(dlq))) --> default[E, DLQ]

}