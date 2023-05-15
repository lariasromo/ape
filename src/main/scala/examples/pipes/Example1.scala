package examples.pipes

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.{CassandraConfig, JDBCConfig, KafkaConfig}
import examples.models.Transaction
import zio.kafka.consumer.Consumer
import zio.stream.ZStream
import zio.{Chunk, Scope, ZIO, ZIOApp, ZIOAppArgs, ZLayer}

object Example1 extends ZIOApp {

  val reader: Reader[KafkaConfig, Any, Transaction] =
    Ape.readers.kafka[KafkaConfig].avro[Transaction]
    .mapZ(s => s
      .map(_.value()) // Getting the value of the ConsumerRecord
      .filter(_.isDefined) //filtering broken avro records
      .map(_.orNull) // safely getting the options after the above filtering
      .filter(r => Seq("SUCCESS", "SUCCEEDED").contains(r.status)) // filtering specific status values
    )

  val parallelPipes: Pipe[CassandraConfig with JDBCConfig with Any with Scope, Any, Transaction, (Chunk[AsyncResultSet], Chunk[Transaction])] =
    Ape.pipes.cassandra[CassandraConfig].default[Any, Transaction] ++
      Ape.pipes.jdbc[JDBCConfig].default[Any, Transaction]

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