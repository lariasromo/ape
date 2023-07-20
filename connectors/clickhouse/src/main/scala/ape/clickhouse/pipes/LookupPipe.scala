package ape.clickhouse.pipes

import ape.clickhouse.configs.MultiClickhouseConfig
import ape.clickhouse.models.ClickhouseLookupModel
import ape.clickhouse.utils.ClickhouseJDBCUtils.lookupModel2Chunk
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO, ZLayer}

import scala.reflect.ClassTag

protected[clickhouse] class LookupPipe[
  E,
  Config <: MultiClickhouseConfig :Tag,
  Model <: ClickhouseLookupModel[T] :Tag :ClassTag,
  T :ClassTag
] extends ClickhousePipe[Config, E, Model, (Model, Chunk[T])] {
  override protected[this] def pipe(i: ZStream[E, Throwable, Model]):
    ZIO[Config, Throwable, ZStream[E, Throwable, (Model, Chunk[T])]] =
      for {
        conf <- ZIO.service[Config]
      } yield i.mapZIO(lookupModel => for {
        results <- {
          lookupModel2Chunk[Model, T](lookupModel)
            .provideSomeLayer(ZLayer.succeed(conf.chConfigs.head))
        }
      } yield (lookupModel, results) )
}

object LookupPipe {
  def lookup[ E,
    Config <: MultiClickhouseConfig :Tag,
    Model <: ClickhouseLookupModel[T] :Tag :ClassTag, T :ClassTag
  ]: LookupPipe[E, Config, Model, T] = new LookupPipe[E, Config, Model, T]
}