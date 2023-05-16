package com.libertexgroup.readers.cassandra

import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel

import scala.reflect.ClassTag

trait CassandraReaders[Config <: CassandraConfig]{
  def default[T :ClassTag](sql: String)(implicit t: Row => T): Reader[Config, Any, T]
}