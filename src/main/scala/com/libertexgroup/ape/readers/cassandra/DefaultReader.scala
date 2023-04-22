package com.libertexgroup.ape.readers.cassandra

import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.ape.utils.CassandraUtils.query2Chunk
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import zio.stream.ZStream
import zio.{Scope, ZIO}

import scala.reflect.ClassTag

protected[cassandra] class DefaultReader[E1, T <:CassandraModel :ClassTag , Config <: CassandraConfig]
  (sql:String)(implicit t: Row => T) extends CassandraReader[Scope with Config, E1, T] {
  override protected[this] def read: ZIO[Scope with Config, Throwable, ZStream[E1, Throwable, T]] = query2Chunk(sql)
}
