# Alexandria Pipeline Engine

The goal of this project is to generate a common approach when creating new data-consuming services.

An APE pipeline is built on top of the ZIO ZStream interface and allows the user to easily write and read from 
different sources the fastest and easiest possible way.

## Getting started
To create a sample pipeline that reads from Kafka, grabs some fields and saves back to Kafka in avro format do the 
following:

1. Define your input class A (this class can come from a schema registry or produced using an avro schema with some 
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

val reader = Ape.readers.kafkaAvroReader[Message]
        .map(Message2.fromKafka)
        .mapZ(_.filter(_.isDefined).map(_.get))
```
4. Define your writer
```scala
import com.libertexgroup.ape.Ape
import zio.kafka.consumer.Consumer

val writer = Ape.pipes.kafkaAvroWriter[Consumer, Message2]
        .preMap(Message2.toKafka)
```
4. Create your pipeline and run it using the `run` method
```scala
for {
   pipe <- reader --> writer
   _ <- pipe.run
} yield ()
```
--------
More resources
--------
 - ### [List of readers and writers](docs/ReadersWritetsList.md)
 - ### [Examples of pipelines](docs/examples/Example1.md)
 - ### [Description and usage of Readers](docs/Readers.md)
 - ### [Description and usage of Writers](docs/Pipes.md)
 - ### [Description and usage of Config](docs/Configs.md)