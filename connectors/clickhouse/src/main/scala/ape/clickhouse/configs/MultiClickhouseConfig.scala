package ape.clickhouse.configs

import MultiClickhouseConfig.ReplicatedMode
import ape.clickhouse.utils.ClickhouseJDBCUtils.query2Chunk
import zio.{ULayer, ZIO, ZLayer, durationInt}

import java.sql.ResultSet


case class MultiClickhouseConfig(chConfigs: List[ClickhouseConfig]){
  val mode: MultiClickhouseConfig.ReplicatedMode.Value = if(chConfigs.size == 1) ReplicatedMode.Standalone else ReplicatedMode.Cluster
}

object MultiClickhouseConfig {
  case class Node(host_address: String, port: Int)
  object Node {
    implicit val result2Clickstream: ResultSet => Node = rs =>
      Node(
        host_address=rs.getString("host_address"),
        port=rs.getInt("port")
      )
  }

  object ReplicatedMode extends Enumeration {
    type ReplicatedMode = Value
    val Cluster, Standalone = Value
  }

  def getConfigwithSuffixFromEnv(envs: Map[String, String], suffix: String): ClickhouseConfig = ClickhouseConfig(
    host = envs.getOrElse(s"CLICKHOUSE_HOST$suffix", throw new Exception(s"CLICKHOUSE_HOST$suffix variable is missing")),
    port = envs.get(s"CLICKHOUSE_PORT$suffix").map(_.toInt).getOrElse(8123),
    databaseName = envs.getOrElse(s"CLICKHOUSE_DATABASE_NAME$suffix", throw new Exception(s"CLICKHOUSE_DATABASE_NAME$suffix variable is missing")),
    username = envs.getOrElse(s"CLICKHOUSE_USERNAME$suffix", throw new Exception(s"CLICKHOUSE_USERNAME$suffix variable is missing")),
    password = envs.getOrElse(s"CLICKHOUSE_PASSWORD$suffix", throw new Exception(s"CLICKHOUSE_PASSWORD$suffix variable is missing")),
    batchSize = 1000, syncDuration = 5.minutes,
    clusterName = envs.get(s"CLICKHOUSE_CLUSTER_NAME")
  )

  def getCHConfigsFromEnv: ZIO[Any, SecurityException, List[ClickhouseConfig]] = for {
    envs <- zio.System.envs
  } yield {
    if (envs.contains("CLICKHOUSE_HOST")) {
      List(getConfigwithSuffixFromEnv(envs, ""))
    } else if (envs.contains("CLICKHOUSE_HOST_0")) {
      envs
        .filter { case (k, _) => k.contains("CLICKHOUSE_HOST") }
        .map { case (k, _) => k.replace("CLICKHOUSE_HOST", "") }
        .map(suffix => getConfigwithSuffixFromEnv(envs, suffix))
        .toList
    } else throw new Exception("Clickhouse configuration is missing")
  }

  def getChConfigsFromOneNode: ZIO[ClickhouseConfig, Throwable, List[ClickhouseConfig]] =
    for {
      config <- ZIO.service[ClickhouseConfig]
      nodes <- query2Chunk[Node](
        s"""SELECT host_address, 8123 as port
           |FROM system.clusters
           |WHERE cluster = '${config.clusterName.getOrElse(throw new Exception("CLICKHOUSE_CLUSTER_NAME is not set"))}'
           |AND replica_num = 1"""
          .stripMargin
      )
      _ <- ZIO.when(nodes.isEmpty)(throw new Exception("Clickhouse nodes are empty"))
    } yield nodes.map(n =>
      ClickhouseConfig(
        host = n.host_address,
        port = n.port,
        databaseName = config.databaseName,
        username = config.username,
        password = config.password,
        batchSize = config.batchSize,
        syncDuration = config.syncDuration,
        clusterName = config.clusterName
      )).toList

  def makeFromCHConfig(conf: ClickhouseConfig): MultiClickhouseConfig = MultiClickhouseConfig(List(conf))

  def liveFromCHConfig(conf: ClickhouseConfig): ULayer[MultiClickhouseConfig] = ZLayer.succeed(makeFromCHConfig(conf))

  def liveFromCHConfig: ZLayer[ClickhouseConfig, Nothing, MultiClickhouseConfig] =
    ZLayer.fromZIO { ZIO.service[ClickhouseConfig].flatMap(conf => ZIO.succeed(makeFromCHConfig(conf))) }

  def liveFromEnv: ZLayer[Any, Throwable, MultiClickhouseConfig] = ZLayer.fromZIO {
    for {
      configs <- getCHConfigsFromEnv
    } yield MultiClickhouseConfig(configs)
  }

  def liveFromNode: ZLayer[ClickhouseConfig, Throwable, MultiClickhouseConfig] = ZLayer.fromZIO {
    for {
      configs <- getChConfigsFromOneNode
    } yield MultiClickhouseConfig(configs)
  }
}
