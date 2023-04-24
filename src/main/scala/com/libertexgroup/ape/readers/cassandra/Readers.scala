package com.libertexgroup.ape.readers.cassandra

import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.ape.Reader
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel

import scala.reflect.ClassTag

// Readers
protected [readers] class Readers[Config <: CassandraConfig]() {
  def default[EZ, T <:CassandraModel :ClassTag](sql: String)(implicit t: Row => T): Reader[Config, EZ, T] =
      new DefaultReader[EZ, T, Config](sql)
}
