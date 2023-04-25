### Scenario
Records with transactions are sent to a kafka broker, such records include a transaction type, status and clientId reference
Records are written in avro

Example of a record in json just for visual representation, actual records are bytes representing an Avro record compressed using an avro schema

```json
{
  "status": "PENDING",
  "transactionType": "DEPOSIT",
  "clientId": "123456"
}
```

This example consists on a pipeline that will; 
1. Reading bytes from the Kafka message 
2. Convert the message to a case class
3. Apply a simple filter (status=SUCCEEDED or SUCCESS)
4. Write the filtered record to Cassandra and PostgreSQL

```scala
import com.libertexgroup.ape.{Ape, Reader, Writer}
import com.libertexgroup.configs.{CassandraConfig, JDBCConfig, KafkaConfig}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.{ZIO, ZIOAppDefault, ZLayer}
import zio.kafka.consumer.Consumer
import zio.stream.ZStream

object Main extends ZIOAppDefault {

  case class Transaction(status: String, transactionType: String, clientId: String)

  val pipe = Ape.readers.kafka[KafkaConfig].avro[Transaction]
      .mapZ(s => s
        .map(_.value()) // Getting the value of the ConsumerRecord
        .filter(_.isDefined) //filtering broken avro records
        .map(_.orNull) // safely getting the options after the above filtering
        .filter(r => Seq("SUCCESS", "SUCCEEDED").contains(r.status)) // filtering specific status values)
      ) --> (Ape.writers.cassandra[CassandraConfig].default ++ Ape.writers.jdbc[JDBCConfig].default)

  val layer: ZLayer[Any with System, Throwable, KafkaConfig with Consumer with CassandraConfig with JDBCConfig] = 
    (KafkaConfig.live() >+> KafkaConfig.liveConsumer) ++ CassandraConfig.live() ++ JDBCConfig.live()

  override def run = (for {
    _ <- pipe.runDrain
  } yield ()).provideLayer(layer)
}
```