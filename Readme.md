# Alexandria Pipeline Engine

The goal of this project is to generate a common approach when creating new data consuming microservices.

## Getting started
To create a sample pipeline that reads from Kafka, grabs some fields and saves back to Kafka in avro format do the 
following:

1. Define your input class A (this class can come from a schema registry or produced using an avro schema with some 
   online tool)
```scala
case class Message(value:String)
```
2. Define an output class B and a simple transformer A => B
```scala
case class Message2(value:String)
val transformer: Message => Message2 = 
   msg => Message2(value = msg.value)
```
3. Define your reader
```scala
val reader = Ape.readers.kafkaAvroReader[Message]
        .map(transformer)
```
4. Define your writer
```scala
val writer = Ape.writers.kafkaAvroWriter[Message2]
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
 - ### [Description and usage of Writers](docs/Writers.md)
 - ### [Description and usage of Config](docs/Configs.md)