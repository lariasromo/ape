package com.libertexgroup.ape.utils

import com.clickhouse.jdbc.ClickHouseConnection
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.executeQueryMulti
import com.libertexgroup.configs.{ClickhouseConfig, MultiClickhouseConfig}
import org.testcontainers.containers.ClickHouseContainer
import zio.{Task, UIO, ZIO, ZLayer, durationInt}

object ClickhouseContainerService extends TestContainerHelper[ClickHouseContainer] {
  def createDummyTable: ZIO[MultiClickhouseConfig, Nothing, Unit] =
    runScoped("CREATE TABLE dummy(a text, b text) ENGINE Log;")

  override val startContainer: Task[ClickHouseContainer] = ZIO.attemptBlocking {
    val container = new ClickHouseContainer("clickhouse/clickhouse-server")
    container.start()
    container
  }

  override val stopContainer: ClickHouseContainer => UIO[Unit] = c => ZIO.succeedBlocking(c.stop())

  def runQuery(con:ClickHouseConnection, query:String) = ZIO.succeed{
    val stmt1 = con.prepareStatement(query)
    stmt1.executeUpdate()
  }

  def runScoped(sql:String): ZIO[MultiClickhouseConfig, Nothing, Unit] = ZIO.scoped(executeQueryMulti(sql))

  def loadSampleData: ZIO[MultiClickhouseConfig, Nothing, Unit] = for {
    _ <- createDummyTable
    _ <- runScoped("INSERT INTO dummy(a, b) VALUES ('value1', 'value2');")
    _ <- runScoped("INSERT INTO dummy(a, b) VALUES ('value3', 'value4');")
  } yield ()

  val clickhouseConfigLayer: ZLayer[ClickHouseContainer, Nothing, MultiClickhouseConfig] = ZLayer.fromZIO(
    for {
      container <- ZIO.service[ClickHouseContainer]
    } yield MultiClickhouseConfig.makeFromCHConfig(
      ClickhouseConfig(
        batchSize=100,
        syncDuration=1.minute,
        host=container.getHost,
        port=container.getMappedPort(8123),
        databaseName="default",
        username=container.getUsername,
        password=container.getPassword
      )
    )
  )

  override val layer: ZLayer[Any, Throwable, MultiClickhouseConfig with ClickHouseContainer] = ZLayer.scoped {
    ZIO.acquireRelease(startContainer)(stopContainer)
  }  >+> clickhouseConfigLayer
}
