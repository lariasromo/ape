package com.libertexgroup.ape.utils

import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.{CqlSession => DatastaxSession}
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel
import palanga.zio.cassandra.CassandraException.SessionOpenException
import palanga.zio.cassandra.ZStatement.StringOps
import palanga.zio.cassandra.{CassandraException, ZCqlSession, session}
import zio.stream.ZStream
import zio.{Scope, ZIO, ZLayer}

import java.net.InetSocketAddress

object CassandraUtils {
  def sessionFromCqlSession[Config <: CassandraConfig]: ZIO[Scope with Config, SessionOpenException, ZCqlSession] =
    for {
      config <- ZIO.service[CassandraConfig]
      con <- session.auto.openFromDatastaxSession({
        var ses = DatastaxSession
          .builder()
          .addContactPoint(new InetSocketAddress(config.host, config.port))
          .withAuthCredentials(config.username, config.password)
        if (config.keyspace.nonEmpty) {
          ses = ses.withKeyspace(config.keyspace)
        }
        ses
          .withLocalDatacenter(config.datacenter)
          .build
      })
    } yield con        

  def layer[Config <: CassandraConfig]: ZLayer[Config, SessionOpenException, ZCqlSession] =
    ZLayer.scoped(sessionFromCqlSession)

  def query2Chunk[Config <: CassandraConfig, T <: CassandraModel](sql:String)(implicit row2Object: Row => T):
    ZIO[Config, SessionOpenException, ZStream[Any, CassandraException, T]] =
      ZIO.scoped[Config] {
        for {
          s <- sessionFromCqlSession[Config]
          stream = ZCqlSession.stream(sql.toStatement.decodeAttempt(row2Object))
            .flatMap(c => ZStream.fromChunk(c))
            .provideSomeLayer(ZLayer.succeed(s))
        } yield stream
      }
}
