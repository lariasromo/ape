package ape.cassandra

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.readers.{CassandraReaders, DefaultReader}
import ape.reader.Reader
import ape.utils.Utils.:=
import com.datastax.oss.driver.api.core.cql.Row
import zio.Tag

import scala.reflect.ClassTag

// Readers
protected [cassandra] class Readers[Config <: CassandraConfig]() extends CassandraReaders[Config]{
  def default[T :ClassTag](sql: String)(implicit t: Row => T): Reader[Config, Any, T] =
    new DefaultReader[Any, T, Config](sql)
}

object Readers {
  def readers[Config <: CassandraConfig : Tag](implicit d1: Config := CassandraConfig) = new Readers[Config]()
}