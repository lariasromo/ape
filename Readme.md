# Alexandria Pipeline Engine

The goal of this project is to generate a common approach when creating new data-consuming services.

An APE pipeline is built on top of the ZIO ZStream interface and allows the user to easily write and read from 
different sources the fastest and easiest possible way.

## Resources
### General descriptions
- [Ape interface](docs/Ape.md)
- [Reader interface](docs/Readers.md)
- [Pipe interface](docs/Pipes.md)
- [Config interface](docs/Configs.md)

### List of available connectors with configs
- [List of Config classes](docs/ConfigList.md)
- [List of Readers](docs/ReaderList.md)
- [List of pipes](docs/PipeList.md)

### Advanced features
- [Pipe operations](docs/PipeOps.md)
- [Read operations](docs/ReaderOps.md)
- [Ape Metrics](docs/Metrics.md)
- [S3 file readers](docs/S3FileReaders.md)
- [Back pressure](docs/BackPressure.md)


## Getting started
To create a sample pipeline that reads from Kafka, grabs some fields and saves back to Kafka in avro format do the 
following:

1. Since our example reads and writes to different kafka topics, possibly different brokers. We need to define 2 
   different config classes.
```scala
import com.libertexgroup.configs.KafkaConfig
import zio.Duration
import zio.kafka.consumer.Consumer.AutoOffsetStrategy

class Kafka1(
        topicName: String,
        kafkaBrokers: List[String],
        consumerGroup: String,
        clientId: String,
        flushSeconds: Duration,
        batchSize: Int
) extends KafkaConfig(topicName,kafkaBrokers,consumerGroup,clientId,flushSeconds,batchSize)
            
class Kafka2(
              topicName: String,
              kafkaBrokers: List[String],
              consumerGroup: String,
              clientId: String,
              flushSeconds: Duration,
              batchSize: Int
) extends KafkaConfig(topicName,kafkaBrokers,consumerGroup,clientId,flushSeconds,batchSize)
```
2. Define your input class A (this class can come from a schema registry or produced using an avro schema with some 
   online tool)
```scala
case class Message(value:String)
```
2. Define an output class B
```scala
case class Message2(value:String)
```
3. We add 2 helper functions `fromKafka` and `toKafka` which will read from the kafka interfaces `ConsumerRecord` 
   and `ProducerRecord` respectively, we add these to `Message2` companion object. We also use an implicit that will 
   help us decode/encode into Avro bytes.
```scala
import com.sksamuel.avro4s.SchemaFor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

object Message2 {
    implicit val schemaFor = SchemaFor[Message2]
    
   val fromKafka: ConsumerRecord[String, Option[Message]] => Option[Message2] =
      consumerRecord => consumerRecord.value().map(msg => Message2(value = msg.value))
   
    val toKafka: Message2 => ProducerRecord[String, Message2] = record => new ProducerRecord("", record)
}
```
5. Define your reader and filters any broken record
```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.KafkaConfig

val reader = Ape.readers.kafka[KafkaConfig].avro[Message]
        .map(Message2.fromKafka)
        .mapZ(_.filter(_.isDefined).map(_.get))
```
4. Define your writer
```scala
import com.libertexgroup.ape.Ape
import zio.kafka.consumer.Consumer

val writer = Ape.pipes.kafka.avro.of[Message2]
        .contramap(Message2.toKafka)
```
4. Create your pipeline and run it using the `run` method
```scala
val main = (reader --> writer).runDrain
```