package ape.jdbc.utils

import ape.jdbc.configs.JDBCConfig
import ape.jdbc.utils.GenericJDBCUtils.executeQuery
import com.dimafeng.testcontainers.PostgreSQLContainer
import zio.{Task, UIO, ZIO, ZLayer, durationInt}


object PostgresContainerService {
  def createDummyTable = runScoped("CREATE TABLE dummy(a text, b text);")

  val startContainer: Task[PostgreSQLContainer] = ZIO.attemptBlocking {
    val container: PostgreSQLContainer = new PostgreSQLContainer()
    container.start()
    container
  }

  val stopContainer: PostgreSQLContainer => UIO[Unit] = c => ZIO.succeedBlocking(c.stop())

  def runScoped(sql:String) = ZIO.scoped(executeQuery[JDBCConfig](sql))

  def loadSampleData: ZIO[JDBCConfig, Throwable, Unit] = for {
      _ <- createDummyTable
      _ <- runScoped("INSERT INTO dummy(a, b) VALUES ('value1', 'value2');")
      _ <- runScoped("INSERT INTO dummy(a, b) VALUES ('value3', 'value4');")
  } yield ()

  val postgresContainer: ZLayer[Any, Throwable, PostgreSQLContainer] = ZLayer.scoped {
    ZIO.acquireRelease(startContainer)(stopContainer)
  }

  def jdbcConfig(parallelism:Int): ZIO[PostgreSQLContainer, Nothing, JDBCConfig] = for {
    container <- ZIO.service[PostgreSQLContainer]
  } yield JDBCConfig(
    batchSize = 5,
    syncDuration = 1.minute,
    driverName=container.driverClassName,
    jdbcUrl=container.jdbcUrl,
    username=container.username,
    password=container.password,
    parallelism = parallelism
  )

  val layer: ZLayer[Any, Throwable, JDBCConfig with PostgreSQLContainer] =
    postgresContainer >+> ZLayer.fromZIO(jdbcConfig(1))
  def layerWithParallelism(parallelism:Int): ZLayer[Any, Throwable, JDBCConfig with PostgreSQLContainer] =
    postgresContainer >+> ZLayer.fromZIO(jdbcConfig(parallelism))
}
