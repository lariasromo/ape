package com.libertexgroup.pipes.cassandra

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs._
import com.libertexgroup.models.cassandra.CassandraModel
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[pipes] class Pipes[Config <: CassandraConfig :Tag]() {
  def default[E, Model <: CassandraModel :Tag :ClassTag]: Pipe[Config, E, Model, Chunk[AsyncResultSet]] =
    new DefaultPipe[E, Config, Model]
}
