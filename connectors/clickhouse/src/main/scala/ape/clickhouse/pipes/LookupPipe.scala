package ape.clickhouse.pipes

import ape.clickhouse.configs.MultiClickhouseConfig
import ape.clickhouse.models.ClickhouseLookupModel
import ape.clickhouse.utils.ClickhouseJDBCUtils.query2ChunkMulti
import ape.pipe.Pipe
import ape.utils.Utils.reLayer
import zio.{Chunk, Tag, ZIO}
import zio.stream.ZStream

import java.sql.ResultSet
import scala.reflect.ClassTag

protected[clickhouse] class LookupPipe[
  E,
  Config <: MultiClickhouseConfig :Tag,
  Model <: ClickhouseLookupModel[T] :Tag :ClassTag,
  T :ClassTag
](implicit r: ResultSet => T) extends ClickhousePipe[Config, E, Model, (Model, Chunk[T])] {
  override protected[this] def pipe(i: ZStream[E, Throwable, Model]):
    ZIO[Config, Throwable, ZStream[E, Throwable, (Model, Chunk[T])]] =
      for {
        rL <- reLayer[Config]
      } yield i.mapZIO(lookupModel => for {
        results <- query2ChunkMulti[T](lookupModel.lookupQuery).provideSomeLayer(rL)
      } yield (lookupModel, results) )
}

object LookupPipe {
  def lookup[Config <: MultiClickhouseConfig :Tag, E, Model <: ClickhouseLookupModel[T] :Tag :ClassTag, T :ClassTag]
    (implicit r: ResultSet => T): LookupPipe[E, Config, Model, T] = new LookupPipe[E, Config, Model, T]
}