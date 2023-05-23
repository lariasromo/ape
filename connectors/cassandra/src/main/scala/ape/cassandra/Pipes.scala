package ape.cassandra

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.models.CassandraModel
import ape.cassandra.pipes.DefaultPipe
import ape.pipe.Pipe
import ape.utils.Utils.:=
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[cassandra] class Pipes[Config <: CassandraConfig :Tag]() {
  def default[E, Model <: CassandraModel :Tag :ClassTag]
  (implicit default: E := Any, default2: Model := CassandraModel):
  Pipe[Config, E, Model, Chunk[AsyncResultSet]] = new DefaultPipe[E, Config, Model]
}

object Pipes {
  def pipes[Config <: CassandraConfig : Tag](implicit d: Config := CassandraConfig) = new Pipes[Config]()
}