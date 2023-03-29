[Readers](src/main/scala/com/libertexgroup/ape/readers)
------
A `Reader` is a component that will start a stream of data using the `ZStream` interface.

Type of stream and environment of the resulting stream and the creation effect are defined in the parent trait `Reader` but can be overriden.

As a good practice is to keep a common trait for similar readers, example of common traits: `KafkaReader` or `S3Reader`
Particular implementations could be `AnodotKafkaReader` or `FxbankReader` if specific behaviour needs to be implemented,
otherwise just direct to the `KafkaDefaultReader` which produces a generic `GenericRecord`. See below...

```scala

//Example of a reader that reads from Kafka
trait KafkaReader extends Reader

object KafkaDefaultReader extends KafkaReader {
  override type Env = Has[KafkaConfig]
  override type Env2 = Any with Consumer with Clock
  override type StreamType = ConsumerRecord[String, Array[Byte]]

  override def apply: ZIO[Env, Throwable, ZStream[Env2, Throwable, StreamType]] = ???
}
```
