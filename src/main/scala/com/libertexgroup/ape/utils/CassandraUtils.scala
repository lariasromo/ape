package com.libertexgroup.ape.utils

import com.datastax.oss.driver.api.core.{CqlSession => DatastaxSession}
import com.libertexgroup.configs.CassandraConfig
import palanga.zio.cassandra.CassandraException.SessionOpenException
import palanga.zio.cassandra.{ZCqlSession, session}
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

}
