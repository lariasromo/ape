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

  val chL: ZLayer[Any, Throwable, PostgreSQLContainer] = ZLayer.scoped {
    ZIO.acquireRelease(startContainer)(stopContainer)
  }

  val jdbcConfig: ZIO[PostgreSQLContainer, Nothing, JDBCConfig] = for {
    container <- ZIO.service[PostgreSQLContainer]
  } yield JDBCConfig(
    batchSize = 1,
    syncDuration = 1.minute,
    driverName=container.driverClassName,
    jdbcUrl=container.jdbcUrl,
    username=container.username,
    password=container.password
  )

  val jdbcConfigLayer: ZLayer[PostgreSQLContainer, Nothing, JDBCConfig] = ZLayer.fromZIO(jdbcConfig)

  val layer: ZLayer[Any, Throwable, JDBCConfig with PostgreSQLContainer] = chL >+> jdbcConfigLayer
}
