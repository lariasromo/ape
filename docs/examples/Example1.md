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
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.{ZIO, ZIOAppDefault}
import zio.kafka.consumer.Consumer
import zio.stream.ZStream

object Main extends ZIOAppDefault {

  case class Transaction(status: String, transactionType: String, clientId: String)

  val reader: Reader[KafkaConfig, Consumer, ConsumerRecord[String, Option[Transaction]]] =
    Ape.readers.kafkaAvroReader[Transaction]

  val validStatuses = Seq("SUCCESS", "SUCCEEDED")

  val filter: ZStream[Consumer, Throwable, Option[Transaction]] => ZStream[Consumer, Throwable, Transaction] = s =>
    s.filter(_.isDefined).map(_.orNull).filter(r => validStatuses.contains(r.status))

//  val pipe = reader --> 
//  (WriterZUnit(filter) --> (Ape.writers.cassandraWriter ++ Ape.writers.jDBCWriter))
  val pipe: ZIO[KafkaConfig, Throwable, Ape[Consumer, Nothing]] = 
    reader --> (Ape.writers.cassandraWriter ++ Ape.writers.jDBCWriter)

  override def run = (for {
    p <- pipe
    _ <- p.run
  } yield ()).provideLayer(KafkaConfig.live >+> KafkaConfig.kafkaConsumer)
}
```