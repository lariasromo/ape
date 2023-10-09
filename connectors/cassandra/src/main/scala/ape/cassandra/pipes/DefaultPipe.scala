package ape.cassandra.pipes

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.models.CassandraModel
import ape.cassandra.utils.CassandraUtils.sessionFromCqlSession
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, SimpleStatement}
import palanga.zio.cassandra.CassandraException
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO, ZLayer}

import scala.reflect.ClassTag

protected[cassandra] class DefaultPipe[E, Config <: CassandraConfig :Tag, Model <: CassandraModel :Tag :ClassTag]
  extends CassandraPipe[Config, E, Model, Chunk[(Model, AsyncResultSet)]] {

  def insertToCassandra(batch: Chunk[Model], config: CassandraConfig): ZIO[Any, CassandraException, Chunk[(Model, AsyncResultSet)]] =
    ZIO.scoped {
      for {
        session <- sessionFromCqlSession[CassandraConfig]
        error <- for {
          script <- session.prepare(SimpleStatement.builder(batch.head.sql).build)
          results <- session.executePar(batch.toList.map(element => element.bind(script)): _*)
          _ <- batch.head.postSql match {
            case Some(postSql) => for {
              postScript <- session.prepare(SimpleStatement.builder(postSql).build)
              _ <- session.executePar(batch.toList.flatMap(element => element.postBind(postScript)): _*)
            } yield ()
            case _ => ZIO.unit
          }
        } yield Chunk.fromIterable(batch.zip(results))
      } yield error
    }.provideSomeLayer(ZLayer.succeed(config))

  override protected[this] def pipe(i: ZStream[E, Throwable, Model]):
    ZIO[Config, Throwable, ZStream[E, Throwable, Chunk[(Model, AsyncResultSet)]]] = for {
    config <- ZIO.service[Config]
    s = i
      .groupedWithin(config.batchSize, config.syncDuration)
      .mapZIO(batch => insertToCassandra(batch, config))
  } yield s
}
