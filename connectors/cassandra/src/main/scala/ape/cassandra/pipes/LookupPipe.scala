package ape.cassandra.pipes

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.models.CassandraLookupModel
import ape.cassandra.utils.CassandraUtils.{lookup, lookupChunk}
import ape.pipe.Pipe
import ape.utils.Utils.reLayer
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO}

import scala.reflect.ClassTag

protected[cassandra] class LookupPipe[E, Config <: CassandraConfig :Tag, Model <: CassandraLookupModel[T] :Tag :ClassTag, T]
  extends CassandraPipe[Config, E, Model, Chunk[(Model, Chunk[T])]] {
  override protected[this] def pipe(i: ZStream[E, Throwable, Model]): ZIO[Config, Throwable, ZStream[E, Throwable, Chunk[(Model, Chunk[T])]]] =
    for {
      cfg <- ZIO.service[Config]
      rL <- reLayer[Config]
    } yield i
      .groupedWithin(cfg.batchSize, cfg.syncDuration)
      .mapZIO(batch => for {
        c <- lookupChunk[Config, T, Model](batch).provideSomeLayer(rL)
      } yield c )
}

object LookupPipe {
  def lookup[Config <: CassandraConfig :Tag, E, Model <: CassandraLookupModel[T] :Tag :ClassTag, T]:
    Pipe[Config, E, Model, (Model, Chunk[T])] =
      new LookupPipe[E, Config, Model, T].mapZ(_.flatMap(x=>ZStream.fromChunk(x)))
}