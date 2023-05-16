package com.libertexgroup.readers.cassandra

import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import com.libertexgroup.utils.CassandraUtils.query2Chunk
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[cassandra] class DefaultReader[EZ, T :ClassTag , Config <: CassandraConfig]
  (sql:String)(implicit t: Row => T) extends CassandraReader[Config, EZ, T] {
  override protected[this] def read: ZIO[Config, Throwable, ZStream[EZ, Throwable, T]] = query2Chunk(sql)
}
