package ape.clickhouse

import ape.clickhouse.configs.MultiClickhouseConfig
import ape.clickhouse.models.{ClickhouseDLQModel, ClickhouseLookupModel, ClickhouseModel}
import ape.clickhouse.pipes.{DefaultPipe, LookupPipe}
import ape.pipe.Pipe
import ape.utils.Utils.:=
import zio.stream.ZStream
import zio.{Chunk, Tag}

import java.sql.{ResultSet, Statement}
import scala.reflect.ClassTag

protected[clickhouse] class Pipes[Config <: MultiClickhouseConfig :Tag]() {

  def default[ET, T <:ClickhouseModel :ClassTag]
  (implicit d1: ET := Any, d2: T := ClickhouseModel):
  Pipe[Config, ET, T, Chunk[(T, Int)]] = new DefaultPipe[ET, T, Config]

  def writerWithDLQ[E, DLQ <: ClickhouseModel :ClassTag, T <:ClickhouseDLQModel[DLQ] :ClassTag]
  (implicit d1: E := Any, d2: DLQ := ClickhouseModel, d3: T := ClickhouseDLQModel[DLQ]):
  Pipe[Config, E, T, Chunk[(DLQ, Int)]] = default[E, T]
    .map(_.filter(_._2.equals(Statement.EXECUTE_FAILED)).map(r => r._1.dlq))
    .mapZ(_.flatMap(dlq => ZStream.fromChunk(dlq))) --> default[E, DLQ]

  def lookup[ET, Model <: ClickhouseLookupModel[T] :Tag :ClassTag, T :ClassTag]
  (implicit d1: ET := Any, d2: T := ClickhouseModel, d3: Model := ClickhouseLookupModel[ClickhouseModel])=
    new LookupPipe[ET, Config, Model, T]()
}

object Pipes {
  def pipes[Config <: MultiClickhouseConfig :Tag](implicit d: Config := MultiClickhouseConfig) = new Pipes[Config]()
}