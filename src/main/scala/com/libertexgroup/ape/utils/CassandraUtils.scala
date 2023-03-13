package com.libertexgroup.ape.utils

import com.libertexgroup.configs.CassandraConfig
import palanga.zio.cassandra.CassandraException.SessionOpenException
import palanga.zio.cassandra.{ZCqlSession, session}
import zio.{Scope, ZIO, ZLayer}
import com.datastax.oss.driver.api.core.{CqlSession => DatastaxSession}

import java.net.InetSocketAddress

object CassandraUtils {
  val sessionFromCqlSession: ZIO[Scope with CassandraConfig, SessionOpenException, ZCqlSession] =
    for {
      config <- ZIO.service[CassandraConfig]
      con <- session.auto.openFromDatastaxSession(
        DatastaxSession
          .builder()
          .addContactPoint(new InetSocketAddress(config.host, config.port))
          .withAuthCredentials(config.username, config.password)
          .withKeyspace(config.keyspace)
          .withLocalDatacenter(config.datacenter)
          .build
      )
    } yield con        

  val layer: ZLayer[CassandraConfig, SessionOpenException, ZCqlSession] =
    ZLayer.scoped(sessionFromCqlSession)

}
