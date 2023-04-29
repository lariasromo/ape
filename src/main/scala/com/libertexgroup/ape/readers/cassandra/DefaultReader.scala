package com.libertexgroup.ape.readers.cassandra

import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.ape.utils.CassandraUtils.query2Chunk
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[cassandra] class DefaultReader[EZ, T <:CassandraModel :ClassTag , Config <: CassandraConfig]
  (sql:String)(implicit t: Row => T) extends CassandraReader[Config, EZ, T] {
  override protected[this] def read: ZIO[Config, Throwable, ZStream[EZ, Throwable, T]] = query2Chunk(sql)
}
