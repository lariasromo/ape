# Alexandria Pipeline Engine

The goal of this project is to generate a common approach when creating new data consuming microservices.


## Getting started
To create a sample pipeline that reads from Kafka, grabs some fields and saves back to Kafka in avro format do the 
following:

1. Define your input class A (this class can come from a schema registry or produced using an avro schema with some 
   online tool) and your reader
```scala
import com.libertexgroup.ape.pipelines.Pipeline

case class Message(value:String)
val reader = Pipeline.readers.kafkaAvroReader[Message]
```
2. Define an output class B and a simple transformer A => B
```scala
case class Message2(value:String)
implicit val transformer: Message => Message2 = msg => {
   Message2(value = msg.value)
} 
```
3. Define your writer
```scala
import com.libertexgroup.ape.pipelines.Pipeline
val writer = Pipeline.writers.kafkaAvroWriter[Message2]
```
4. Create your pipeline
```scala
val pipeline = reader --> writer
```
5. Run your pipeline using the `run` method
```scala
for {
   _ <- pipeline.run
} yield ()
```
## List of Readers (v1)
**Reader description below
 - `clickhouseDefaultReader`: Reads from clickhouse database using a sql statement
 - `jdbcDefaultReader`: Reads from jdbc supporting database using a sql statement 
 - `kafkaDefaultReader`: Reads from kafka using a `KafkaConfig`, messages with bytes 
 - `kafkaAvroReader`: Reads from kafka using a `KafkaConfig` layer, upcoming messages are transformed to objects using a 
   case class as reference
 - `kafkaJsonCirceReader`: Reads from kafka using a `KafkaConfig` layer, upcoming messages are transformed to objects using a 
   case class as reference
 - `kafkaStringReader`: Reads from kafka using a `KafkaConfig`, upcoming messages are transformed to objects using a case
   class as reference 
 - `s3ParquetReader`: Reads parquet files from S3
 - `s3AvroReader`: Reads avro datum files from S3
 - `s3TextReader`: Reads plaintext from S3
 - `S3JsonLinesReader`: Reads text files to S3 that contain a json representation per line, it needs to have an
  implicit conversion `String => T`
 - `S3JsonLinesCirceWriter`:Reads text files to S3 that contain a json representation per line, to encode
  object T to json string it uses circe and there has to be an implicit instance of `io.circe.Encoder`
 - `websocketReader`: Reads from a websocket (requires a `Websocket[Task]` object) see https://sttp.softwaremill.com/en/latest/examples.html#open-a-websocket-using-zio
 - 
## List of Writers (v1)
**Writer description below
 - `clickhouseWriter`: Writes to a Clickhouse table using `ClickhouseConfig`, which allows writes grouping by batch 
   size or time. The input models need to implement the `CLickhouseModel` interface.
 - `jDBCWriter`: Writes to a jdbc supporting table using `JDBCConfig`, which allows writes grouping by batch
   size or time. The input models need to implement the `JDBCModel` interface.
 - `kafkaStringWriter`: Writes strings to kafka
 - `kafkaAvroWriter`: Writes avro bytes to kafka
 - `s3AvroWriter`: Writes avro bytes to S3
 - `S3JsonLinesWriter`: Writes entities to text files to S3 with a json representation per line, it needs to have an 
   implicit conversion `String => T`
 - `S3JsonLinesCirceWriter`: Writes entities to text files to S3 with a json representation per line, to encode 
   object T to json string it uses circe and there has to be an implicit instance of `io.circe.Encoder`
 - `s3ParquetWriter`: Writes parquet files to S3
 - `s3TextWriter`: Writes text files to S3
 - `consoleWriter`/`consoleStringWriter`: Writes the output to the console (useful for testing purposes)


[Readers](src/main/scala/com/libertexgroup/ape/readers)
------
A `Reader` is a component that will start a stream of data using the `ZStream` interface.

Type of stream and environment of the resulting stream and the creation effect are defined in the parent trait `Reader` but can be overriden.

As a good practice is to keep a common trait for similar readers, example of common traits: `KafkaReader` or `S3Reader` 
Particular implementations could be `AnodotKafkaReader` or `FxbankReader` if specific behaviour needs to be implemented, 
otherwise just direct to the `KafkaDefaultReader` which produces a generic `GenericRecord`. See below...

```scala
import com.libertexgroup.ape.readers.Reader

//Example of a reader that reads from Kafka
trait KafkaReader extends Reader

object KafkaDefaultReader extends KafkaReader {
  override type Env = Has[KafkaConfig]
  override type Env2 = Any with Consumer with Clock
  override type StreamType = ConsumerRecord[String, Array[Byte]]

  override def apply: ZIO[Env, Throwable, ZStream[Env2, Throwable, StreamType]] = ???
}
```

[Transformers](src/main/scala/com/libertexgroup/ape/transformers)
------
Following the same pattern as the reader interface we have the Transformers interface.

This interface contains an `apply` method that takes a ZStream of type `I` and produces a new ZStream of type `OutputType`

```scala
import com.libertexgroup.ape.transformers.Transformer

class DefaultTransformer[E, I] extends Transformer[E, I] {
  type OutputType = Array[Byte]

  override def apply(stream: ZStream[E, Throwable, I]): ZStream[E, Throwable, OutputType] = ???
}
```

[Writers](src/main/scala/com/libertexgroup/ape/writers)
------

A writer has a main method `apply` that uses a stream of type `InputType` and stores this stream effect-fully with an environment of type `EnvType` and able to fail with a `Throwable` side effect 

A good practice (like we do with readers) we keep a common trait for writers to the same target e.g. `ClickhouseWriter`, `S3Writer`, `KafkaWriter`, etc.
In most cases the same config we use for reading from a source can be reused to write to a target.

Example...

```scala
import com.libertexgroup.ape.writers.Writer

trait S3Writer[T] extends Writer[T]

class DefaultWriter[E] extends S3Writer[E] {
  override type EnvType = s3.S3 with E with Has[S3Config]
  override type InputType = Array[Byte]

  override def apply(stream: ZStream[E, Throwable, InputType]): ZIO[EnvType, Throwable, Unit] = ???
}
```

# [Configs](src/main/scala/com/libertexgroup/configs)

We can keep reusable configuration case classes associated to a source or target e.g. `JDBCConfig`, `S3Config`, `ClickhouseConfig` etc.

We also keep have a [common config](src/main/scala/com/libertexgroup/configs/ProgramConfig.scala) (`ProgramConfig`) class for the whole pipeline that contains information about which reader or writer should be use.
```scala
case class ProgramConfig(
                             reader: String,
                             transformer: String,
                             streamConfig: Option[StreamConfig],
                             writer: String
                           )
```

Example of a config to connect to a source or target (kafka)
```scala
case class KafkaConfig(
                        topicName: String,
                        kafkaBrokers: List[String],
                        consumerGroup: String,
                        flushSeconds: Duration,
                        batchSize: Int
                      )
```
This project also includes `live` methods that creates an instance of these config classes using environment variables as sources.

***It is important that when we create layers with config classes we use default values since the environment can keep growing
### Real life example 
[PipelineExample](src/main/scala/com/libertexgroup/PipelineExample.scala)
```scala
  def main: ZIO[Clock with Blocking with system.System with Console, Throwable, Unit] = for {
    kLayer <- KafkaConfig.kafkaConsumer.provideLayer(layer)
    _ <- DefaultPipeline.run.provideLayer(layer ++ kLayer ++ ProgramConfig.fromJsonString(configJson))
  } yield ()
```

### Default pipeline
This default pipeline creates an instance of a reader, transformer and writer using the `ProgramConfig` interface.

Use the default pipeline to register more implementation since reading them from a config file is handy, if needed we can have more pipelines so the environment types don't grow that much and we keep the code cleaner.