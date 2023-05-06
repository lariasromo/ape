package com.libertexgroup.pipes.cassandra

import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, SimpleStatement}
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import com.libertexgroup.utils.CassandraUtils.sessionFromCqlSession
import palanga.zio.cassandra.CassandraException
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO, ZLayer}

import scala.reflect.ClassTag

protected[cassandra] class DefaultPipe[E, Config <: CassandraConfig :Tag, Model <: CassandraModel :Tag :ClassTag]
  extends CassandraPipe[Config, E, Model, Chunk[AsyncResultSet]] {

  def insertToCassandra(batch: Chunk[Model], config: CassandraConfig): ZIO[Any, CassandraException, Chunk[AsyncResultSet]] =
    ZIO.scoped {
      for {
        session <- sessionFromCqlSession[CassandraConfig]
        error <- for {
          ps <- session.prepare(SimpleStatement.builder(batch.head.sql).build)
          results <- session.executePar(batch.toList.map(element => element.bind(ps)): _*)
        } yield Chunk.fromIterable(results)
      } yield error
    }.provideSomeLayer(ZLayer.succeed(config))

  override protected[this] def pipe(i: ZStream[E, Throwable, Model]):
    ZIO[Config, Throwable, ZStream[E, Throwable, Chunk[AsyncResultSet]]] = for {
    config <- ZIO.service[Config]
    s = i
      .groupedWithin(config.batchSize, config.syncDuration)
      .mapZIO(batch => insertToCassandra(batch, config))
  } yield s
}
