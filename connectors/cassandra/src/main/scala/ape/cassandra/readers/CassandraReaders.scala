package ape.cassandra.readers

import ape.cassandra.configs.CassandraConfig
import ape.reader.Reader
import com.datastax.oss.driver.api.core.cql.Row

import scala.reflect.ClassTag

trait CassandraReaders[Config <: CassandraConfig]{
  def default[T :ClassTag](sql: String)(implicit t: Row => T): Reader[Config, Any, T]
}