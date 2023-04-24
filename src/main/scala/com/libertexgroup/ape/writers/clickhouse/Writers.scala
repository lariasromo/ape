package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.ape.{Ape, Writer}
import com.libertexgroup.configs._
import com.libertexgroup.models.clickhouse.{ClickhouseDLQModel, ClickhouseModel}
import zio.stream.ZStream
import zio.{Chunk, Tag}

import java.sql.Statement
import scala.reflect.ClassTag

protected[writers] class Writers[Config <: MultiClickhouseConfig :Tag]() {

  def default[ET, T <:ClickhouseModel :ClassTag]: Writer[Config, ET, T, Chunk[(T, Int)]] =
    new DefaultWriter[ET, T, Config]

  def writerWithDLQ[E, DLQ <: ClickhouseModel :ClassTag, T <:ClickhouseDLQModel[DLQ] :ClassTag]:
    Writer[Config, E, T, Chunk[(DLQ, Int)]] = default[E, T]
    .map(_.filter(_._2.equals(Statement.EXECUTE_FAILED)).map(r => r._1.dlq))
    .mapZ(_.flatMap(dlq => ZStream.fromChunk(dlq))) --> default[E, DLQ]

}
