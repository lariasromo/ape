package ape.cassandra.configs

import zio.System.envOrElse
import zio.{ZIO, ZLayer, durationInt}

import scala.util.Try
import zio.config.ZConfig
import zio.config.magnolia.descriptor

import java.net.InetSocketAddress

case class CassandraConfig(
                            hosts: List[InetSocketAddress],
                            keyspace: String,
                            batchSize: Int,
                            syncDuration: zio.Duration,
                            username: String,
                            password: String,
                            datacenter: String="datacenter1",
                          )

object CassandraConfig {
//  val configDescriptor = descriptor[CassandraConfig]
//  val liveMagnolia: ZLayer[Any, Throwable, CassandraConfig] = ZConfig.fromSystemEnv(configDescriptor)

  def live(prefix:Option[String]=None): ZLayer[Any, SecurityException, CassandraConfig] = ZLayer(make(prefix))

  def make(prefix:Option[String]=None): ZIO[Any, SecurityException, CassandraConfig] = for {
    host <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_HOST",
      throw new Exception(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_HOST needs to be set"))
    keyspace <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_KEYSPACE", "")
    port <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_PORT", "9042")
    batchSize <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_BATCHSIZE", "10000")
    syncDuration <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_SYNCDURATION", "5")
    username <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_USERNAME", "")
    password <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_PASSWORD", "")
    datacenter <- envOrElse(prefix.map(s=>s+"_").getOrElse("") + "CASSANDRA_DATACENTER", "datacenter1")
  } yield CassandraConfig(
    hosts=List(new InetSocketAddress(host, Try(port.toInt).toOption.getOrElse(9042))),
    keyspace=keyspace,
    batchSize=Try(batchSize.toInt).toOption.getOrElse(10000),
    syncDuration=Try(syncDuration.toInt.minutes).toOption.getOrElse(5.minutes),
    username=username,
    password=password,
    datacenter=datacenter,
  )
}