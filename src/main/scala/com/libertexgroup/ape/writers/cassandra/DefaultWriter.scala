package com.libertexgroup.ape.writers.cassandra

import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.libertexgroup.ape.utils.CassandraUtils.sessionFromCqlSession
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import zio.stream.{ZSink, ZStream}
import zio.{Scope, ZIO}

protected[writers] class DefaultWriter[E] extends CassandraWriter[E, E with Scope with CassandraConfig, CassandraModel] {
  override def apply(stream: ZStream[E, Throwable, CassandraModel]):
                      ZIO[Scope with CassandraConfig with E, Throwable, Unit] = for {
      config <- ZIO.service[CassandraConfig]
      _ <- stream
        .groupedWithin(config.batchSize, config.syncDuration)
        .mapZIO(batch => for {
          session <- sessionFromCqlSession
          error <- ZIO.scoped( for {
            ps <- session.prepare(SimpleStatement.builder(batch.head.sql).build)
            _ <- session.executePar(batch.toList.map(element => element.bind(ps)): _*)
          } yield () )
        } yield error )
        .runScoped(ZSink.drain)
    } yield ()
}
