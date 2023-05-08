package com.libertexgroup.readers.cassandra

import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import com.libertexgroup.readers.clickhouse.Readers

import java.sql.ResultSet
import scala.reflect.ClassTag

// Readers
protected [readers] class Readers[Config <: CassandraConfig]() extends CassandraReaders[Config]{
  def default[T <:CassandraModel :ClassTag](sql: String)(implicit t: Row => T): Reader[Config, Any, T] =
      new DefaultReader[Any, T, Config](sql)
}
