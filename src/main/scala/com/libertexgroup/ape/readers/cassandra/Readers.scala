package com.libertexgroup.ape.readers.cassandra

import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.ape.Reader
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import zio.Scope

import scala.reflect.ClassTag

// Readers
protected [cassandra] class Readers[Config <: CassandraConfig]() {
  def default[E, T <:CassandraModel :ClassTag](sql: String)(implicit t: Row => T): Reader[Scope with Config, E, T] =
      new DefaultReader[E, T, Config](sql)
}
