
## Getting started
To create a sample pipeline that reads from Kafka, grabs some fields and saves back to Kafka in avro format do the
following:

1. Since our example reads and writes to different kafka topics, possibly different brokers. We need to define 2
   different config classes.
2. Define your input class A (this class can come from a schema registry or produced using an avro schema with some
   online tool)
3. Define an output class B
4. We add 2 helper functions `fromKafka` and `toKafka` which will read from the kafka interfaces `ConsumerRecord`
   and `ProducerRecord` respectively, we add these to `Message2` companion object. We also use an implicit that will
   help us decode/encode into Avro bytes.
5. Define your reader and filters any broken record
6. Define your writer
7. Create your pipeline and run it using the `run` method

```scala
import ape.kafka.configs.KafkaConfig
import ape.pipe.Pipe
import ape.reader.Reader
import com.sksamuel.avro4s.SchemaFor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Duration, ZIO}
import zio.kafka.consumer.Consumer.AutoOffsetStrategy

class Kafka1(
                    topicName: String,
                    kafkaBrokers: List[String],
                    consumerGroup: String,
                    clientId: String
            ) extends KafkaConfig(topicName, kafkaBrokers, consumerGroup, clientId)

class Kafka2(
                    topicName: String,
                    kafkaBrokers: List[String],
                    consumerGroup: String,
                    clientId: String,
                    flushSeconds: Duration,
                    batchSize: Int
            ) extends KafkaConfig(topicName, kafkaBrokers, consumerGroup, clientId, flushSeconds, batchSize)

case class Message1(value: String)

case class Message2(value: String)

object Message2 {
   val fromKafka: ConsumerRecord[String, Option[Message1]] => Option[Message2] =
      consumerRecord => consumerRecord.value().map(msg => Message2(value = msg.value))

   val toKafka: Message2 => ProducerRecord[String, Message2] = record => new ProducerRecord("", record)
}

type KafkaRecord = ProducerRecord[String, Message2]

val reader: Reader[KafkaConfig, Any, KafkaRecord] =
   ape.kafka.Readers
           .readers[KafkaConfig]
           .avro[Message1]
           .map(Message2.fromKafka)
           .safeGet[Message2]
           .map(Message2.toKafka)

val writer: Pipe[Nothing, Any, KafkaRecord, KafkaRecord] =
   ape.kafka.Pipes
           .pipes
           .avro.of[Message2]

val main: ZIO[Any with KafkaConfig with Nothing, Throwable, Unit] = (reader --> writer).runDrain
```