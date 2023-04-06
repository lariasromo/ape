package com.libertexgroup.ape.writers.cassandra

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.libertexgroup.ape.Writer
import com.libertexgroup.configs._
import com.libertexgroup.models.cassandra.CassandraModel
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[writers] class Writers[Config <: CassandraConfig :Tag]() {
  def default[E, Model <: CassandraModel :Tag :ClassTag]: Writer[Config, E, Model, Chunk[AsyncResultSet]] =
    new DefaultWriter[E, Config, Model]
}
