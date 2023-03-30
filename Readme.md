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
--------
More resources
--------
 - ### [List of readers and writers](docs/ReadersWritetsList.md)
 - ### [Examples of pipelines](docs/examples/Example1.md)
 - ### [Description and usage of Readers](docs/Readers.md)
 - ### [Description and usage of Writers](docs/Writers.md)
 - ### [Description and usage of Config](docs/Configs.md)