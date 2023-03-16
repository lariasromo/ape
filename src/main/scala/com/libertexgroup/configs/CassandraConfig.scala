package com.libertexgroup.configs

import zio.System.envOrElse
import zio.{ZIO, ZLayer, durationInt}

import scala.util.Try

case class CassandraConfig(
                            host: String,
                            keyspace: String,
                            port: Int,
                            batchSize: Int,
                            syncDuration: zio.Duration,
                            username: String,
                            password: String,
                            datacenter: String="datacenter1",
                          )

object CassandraConfig {
  def live: ZLayer[Any, SecurityException, CassandraConfig] = ZLayer(make)

  def make: ZIO[Any, SecurityException, CassandraConfig] = for {
    host <- envOrElse("CASSANDRA_HOST", throw new Exception("CASSANDRA_HOST needs to be set"))
    keyspace <- envOrElse("CASSANDRA_KEYSPACE", "")
    port <- envOrElse("CASSANDRA_PORT", "9042")
    batchSize <- envOrElse("CASSANDRA_BATCHSIZE", "10000")
    syncDuration <- envOrElse("CASSANDRA_SYNCDURATION", "5")
    username <- envOrElse("CASSANDRA_USERNAME", "")
    password <- envOrElse("CASSANDRA_PASSWORD", "")
    datacenter <- envOrElse("CASSANDRA_DATACENTER", "datacenter1")
  } yield CassandraConfig(
    host=host,
    keyspace=keyspace,
    port=Try(port.toInt).toOption.getOrElse(9042),
    batchSize=Try(batchSize.toInt).toOption.getOrElse(10000),
    syncDuration=Try(syncDuration.toInt.minutes).toOption.getOrElse(5.minutes),
    username=username,
    password=password,
    datacenter=datacenter,
  )
}