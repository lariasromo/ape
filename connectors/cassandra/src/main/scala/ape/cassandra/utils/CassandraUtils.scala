package ape.cassandra.utils

import ape.cassandra.configs.CassandraConfig
import ape.cassandra.models.CassandraLookupModel
import com.datastax.oss.driver.api.core.cql.{Row, SimpleStatement}
import com.datastax.oss.driver.api.core.{CqlSession => DatastaxSession}
import palanga.zio.cassandra.CassandraException.SessionOpenException
import palanga.zio.cassandra.ZStatement.StringOps
import palanga.zio.cassandra.{CassandraException, ZCqlSession, ZSimpleStatement, session}
import zio.stream.ZStream
import zio.{Chunk, Scope, Tag, ZIO, ZLayer}

import java.net.InetSocketAddress
import scala.reflect.ClassTag

object CassandraUtils {
  def lookupChunk[Config <: CassandraConfig, T, Model <: CassandraLookupModel[T] :Tag :ClassTag](lookupChunk: Chunk[Model]):
    ZIO[Config, Throwable, Chunk[(Model, Chunk[T])]] = ZIO.scoped[Config] {
    for {
      res <- lookupChunk.mapZIO { lookup[Config, T, Model] }
    } yield res
  }

  def lookup[Config <: CassandraConfig, T, Model <: CassandraLookupModel[T] :Tag :ClassTag](lookup: Model):
    ZIO[Scope with Config, Throwable, (Model, Chunk[T])] = for {
    s <- sessionFromCqlSession[Config]
    chunk <- {
      ZCqlSession.stream ( {
        val zS = new ZSimpleStatement[T] (
          statement = SimpleStatement.builder (lookup.lookupQuery).setTimeout(lookup.timeout).build (),
          bindInternal = pS => lookup.lookupBind (pS),
          decodeInternal = row => Right (lookup.lookupDecode (row))
        )
        zS
      } )
        .runCollect
        .provideSomeLayer (ZLayer.succeed (s))
    }
  } yield (lookup, chunk.flatten)

  def sessionFromCqlSession[Config <: CassandraConfig]: ZIO[Scope with Config, SessionOpenException, ZCqlSession] =
    for {
      config <- ZIO.service[CassandraConfig]
      con <- session.auto.openFromDatastaxSession({
        var ses = DatastaxSession
          .builder()
          .addContactPoint(new InetSocketAddress(config.host, config.port))
          .withAuthCredentials(config.username, config.password)
        if (config.keyspace.nonEmpty) {
          ses = ses.withKeyspace(config.keyspace)
        }
        ses
          .withLocalDatacenter(config.datacenter)
          .build
      })
    } yield con

  def layer[Config <: CassandraConfig]: ZLayer[Config, SessionOpenException, ZCqlSession] =
    ZLayer.scoped(sessionFromCqlSession)

  def query2Chunk[Config <: CassandraConfig, T](sql: String)(implicit row2Object: Row => T):
  ZIO[Config, CassandraException, ZStream[Any, Nothing, T]]
  = ZIO.scoped[Config] {
    for {
      s <- sessionFromCqlSession[Config]
      stream <- ZCqlSession.stream(sql.toStatement.decodeAttempt(row2Object))
        .flatMap(c => ZStream.fromChunk(c))
        .provideSomeLayer(ZLayer.succeed(s))
        .runCollect
    } yield ZStream.fromChunk(stream)
  }

}
