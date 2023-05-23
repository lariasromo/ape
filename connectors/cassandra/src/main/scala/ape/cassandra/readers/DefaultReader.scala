package ape.cassandra.readers

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.utils.CassandraUtils.query2Chunk
import com.datastax.oss.driver.api.core.cql.Row
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[cassandra] class DefaultReader[EZ, T :ClassTag , Config <: CassandraConfig]
  (sql:String)(implicit t: Row => T) extends CassandraReader[Config, EZ, T] {
  override protected[this] def read: ZIO[Config, Throwable, ZStream[EZ, Throwable, T]] = query2Chunk(sql)
}
