package pipes

import ape.cassandra.configs.CassandraConfig
import ape.jdbc.configs.JDBCConfig
import ape.kafka.configs.KafkaConfig
import ape.pipe.Pipe
import ape.reader.Reader
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import models.Transaction
import zio.kafka.consumer.Consumer
import zio.stream.ZStream
import zio.{Chunk, Scope, ZIO, ZIOApp, ZIOAppArgs, ZLayer}

object Example1 extends ZIOApp {

  val reader: Reader[KafkaConfig, Any, Transaction] =
    ape.kafka.Readers.readersFlattened[KafkaConfig].avro[Transaction]
    .mapZ(s => s
      .map(_.value()) // Getting the value of the ConsumerRecord
      .filter(_.isDefined) //filtering broken avro records
      .map(_.orNull) // safely getting the options after the above filtering
      .filter(r => Seq("SUCCESS", "SUCCEEDED").contains(r.status)) // filtering specific status values
    )

  val parallelPipes: Pipe[CassandraConfig with JDBCConfig with Any with Scope, Any, Transaction, (Chunk[AsyncResultSet], Chunk[Transaction])] =
    ape.cassandra.Pipes.pipes[CassandraConfig].default[Any, Transaction] ++
      ape.jdbc.Pipes.pipes[JDBCConfig].default[Any, Transaction]

  val pipe: ZStream[Any with KafkaConfig with CassandraConfig with JDBCConfig with Scope, Throwable, (Chunk[AsyncResultSet], Chunk[Transaction])] = reader --> parallelPipes

  val layer: ZLayer[Any with System, Throwable, KafkaConfig with Consumer with CassandraConfig with JDBCConfig] =
    (KafkaConfig.live() >+> KafkaConfig.liveConsumer) ++ CassandraConfig.live() ++ JDBCConfig.live()

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    pipe.runDrain

  override implicit def environmentTag: zio.EnvironmentTag[Environment] = zio.EnvironmentTag[Environment]

  override type Environment = KafkaConfig with CassandraConfig with JDBCConfig

  override def bootstrap: ZLayer[ZIOAppArgs, Any, Environment] =
    KafkaConfig.live() ++ CassandraConfig.live() ++ JDBCConfig.live()
}