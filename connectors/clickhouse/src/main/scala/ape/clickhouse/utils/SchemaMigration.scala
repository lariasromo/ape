package ape.clickhouse.utils

import ape.clickhouse.configs.MultiClickhouseConfig.ReplicatedMode
import ape.clickhouse.configs.{ClickhouseConfig, MultiClickhouseConfig}
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import zio.System.env
import zio.{ZIO, ZLayer}

import java.sql.Connection

/**
 * Runs Liquibase based database schema and data migrations. This is the only place for all related
 * modules to run updates from.
 *
 * Liquibase finds its files on the classpath and applies them to DB. If migration fails
 * this class will throw an exception and by default your application should not continue to run.
 *
 * It does not matter which module runs this migration first.
 */
class SchemaMigration {

  def createLiquibase(masterChangeLogFile: String, dbName: String, dbConnection: Connection, liquibaseSuffix:Option[String]): Liquibase = {
    val database = DatabaseFactory.getInstance.findCorrectDatabaseImplementation(new JdbcConnection(dbConnection))
    val classLoader = classOf[SchemaMigration].getClassLoader
    val resourceAccessor = new ClassLoaderResourceAccessor(classLoader)
    database.setDefaultSchemaName(dbName)
    database.setDatabaseChangeLogTableName(
      "DATABASECHANGELOG" + liquibaseSuffix.map(s => s"_${s}").getOrElse("")
    )
    database.setDatabaseChangeLogLockTableName(
      "DATABASECHANGELOGLOCK" + liquibaseSuffix.map(s => s"_${s}").getOrElse("")
    )
    new Liquibase(masterChangeLogFile, resourceAccessor, database)
  }

  def updateDb(path:String): ZIO[ClickhouseConfig, Throwable, Unit] = ZIO.scoped {
    for {
      liquibaseSuffix <- env("LIQUIBASE_SUFFIX")
      config <- ZIO.service[ClickhouseConfig]
      connection <- ClickhouseJDBCUtils.connect
      liquibase <- ZIO.succeed(createLiquibase(path, config.databaseName, connection, liquibaseSuffix))
      _ <- ZIO.attempt {
        liquibase.update("")
        liquibase.forceReleaseLocks()
      }
    } yield ()
  }

  /**
   * Invoke this method to apply all DB migrations.
   */
  def run: ZIO[MultiClickhouseConfig, Throwable, Unit] =
    for {
      mcConfig <- ZIO.service[MultiClickhouseConfig]
      _ <- ZIO.logInfo(s"Liquibase running")
      config <- ZIO.service[MultiClickhouseConfig]
      root = mcConfig.mode match {
        case ReplicatedMode.Cluster => "/liquibase/changelog/sql/cluster/root-changelog.xml"
        case ReplicatedMode.Standalone => "/liquibase/changelog/sql/standalone/root-changelog.xml"
      }
      _ <- updateDb(root).provideSomeLayer(ZLayer.succeed(config.chConfigs.head))
    } yield ()

}

object SchemaMigration extends SchemaMigration