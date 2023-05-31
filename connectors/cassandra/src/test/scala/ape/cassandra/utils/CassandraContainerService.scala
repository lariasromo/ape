package ape.cassandra.utils

import ape.cassandra.configs.CassandraConfig
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, SimpleStatement}
import com.dimafeng.testcontainers.CassandraContainer
import palanga.zio.cassandra.CassandraException
import zio.{Task, UIO, ZIO, ZLayer, durationInt}

object CassandraContainerService {
  def createKeyspaceStmt(keyspace:String) = SimpleStatement
      .builder(
        s"""
           |CREATE KEYSPACE ${keyspace}
           |  WITH REPLICATION = {
           |   'class' : 'SimpleStrategy',
           |   'replication_factor' : 1
           |  };
           |""".stripMargin
      )
      .build

  val createTableStmt = SimpleStatement
      .builder(
        s"""
           |CREATE TABLE IF NOT EXISTS dummy (
           |  a text,
           |  b text,
           |  PRIMARY KEY (a, b)
           |);
           |""".stripMargin
      )
      .build

  def createKeyspace(keyspace: String): ZIO[CassandraConfig, CassandraException, Unit] = for {
    _ <- ZIO.scoped( for {
      session <- CassandraUtils.sessionFromCqlSession[CassandraConfig]
      _ <- session.execute(createKeyspaceStmt(keyspace))
    } yield ())
  } yield ()

  val createTable: ZIO[CassandraConfig, CassandraException, AsyncResultSet] = ZIO.scoped(
    CassandraUtils.sessionFromCqlSession[CassandraConfig].flatMap(session => session.execute(createTableStmt))
  )

  val startContainer: Task[CassandraContainer] = ZIO.attemptBlocking {
    val container = new CassandraContainer()
    container.start()
    container
  }

  val stopContainer: CassandraContainer => UIO[Unit] = c => ZIO.succeedBlocking(c.stop())


  val cassandraConfigLayer: ZLayer[CassandraContainer, Throwable, CassandraConfig] = ZLayer.fromZIO(
    for {
      container <- ZIO.service[CassandraContainer]
      config = CassandraConfig(
        batchSize=1,
        syncDuration=1.minute,
        host=container.host,
        port=container.mappedPort(9042),
        keyspace="",
        username=container.username,
        password=container.password
      )
      keyspace = "test"
      _ <- createKeyspace(keyspace).provideSomeLayer(ZLayer.succeed(config))
    } yield config.copy(keyspace = "test")
  )

  val layer: ZLayer[Any, Throwable, CassandraConfig with CassandraContainer] = ZLayer.scoped {
    ZIO.acquireRelease(startContainer)(stopContainer)
  }  >+> cassandraConfigLayer
}
