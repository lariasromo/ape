package com.libertexgroup.ape.writers.cassandra

import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, SimpleStatement}
import com.libertexgroup.ape.utils.CassandraUtils.sessionFromCqlSession
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import palanga.zio.cassandra.CassandraException
import zio.stream.ZStream
import zio.{Chunk, ZIO, ZLayer}

protected[writers] class DefaultWriter[E] extends CassandraWriter[CassandraConfig, E, CassandraModel, Chunk[AsyncResultSet]] {
  def insertToCassandra(batch: Chunk[CassandraModel], config: CassandraConfig): ZIO[Any, CassandraException, Chunk[AsyncResultSet]] =
    (
      ZIO.scoped {
        for {
          session <- sessionFromCqlSession
          error <- for {
            ps <- session.prepare(SimpleStatement.builder(batch.head.sql).build)
            results <- session.executePar(batch.toList.map(element => element.bind(ps)): _*)
          } yield Chunk.fromIterable(results)
        } yield error
      }
    ).provideSomeLayer(ZLayer.succeed(config))


  override def apply(stream: ZStream[E, Throwable, CassandraModel]):
  ZIO[CassandraConfig, Throwable, ZStream[E, Throwable, Chunk[AsyncResultSet]]] =
    for {
      config <- ZIO.service[CassandraConfig]
      s = stream
        .groupedWithin(config.batchSize, config.syncDuration)
        .mapZIO(batch => insertToCassandra(batch, config))
    } yield s
}
