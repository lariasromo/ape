package ape.cassandra

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.models.{CassandraLookupModel, CassandraModel}
import ape.cassandra.pipes.{DefaultPipe, LookupPipe}
import ape.pipe.Pipe
import ape.utils.Utils.:=
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import zio.stream.ZStream
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[cassandra] class Pipes[Config <: CassandraConfig :Tag]() {
  def default[E, Model <: CassandraModel :Tag :ClassTag]
    (implicit default: E := Any, default2: Model := CassandraModel): Pipe[Config, E, Model, Chunk[AsyncResultSet]] =
    new DefaultPipe[E, Config, Model]

  def lookup[E, T, Model <: CassandraLookupModel[T] :Tag :ClassTag]: Pipe[Config, E, Model, (Model, Chunk[T])] =
    new LookupPipe[E, Config, T, Model].mapZ(_.flatMap(x=>ZStream.fromChunk(x)))
}

object Pipes {
  def pipes[Config <: CassandraConfig : Tag](implicit d: Config := CassandraConfig) = new Pipes[Config]()
}