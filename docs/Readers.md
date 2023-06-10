# Readers
 - [List of Readers](ReaderList.md)
 - [Reader operations](ReaderOps.md)

## Description
A `Reader` is a component that will start a stream of data using the `ZStream` interface.

```text


   ┌────────────────┐       ┌─────────────────┐      
   │                │       │                 │      
   │                │       │                 │      
   │     Reader     │  T0   │     Pipe 1      │ T1   
   │                ├───────►                 ├─────►
   │                │       │                 │      
   │                │       │                 │      
   └────────────────┘       └─────────────────┘      
```

Type of stream and environment of the resulting stream and the creation effect are defined in the parent trait `Reader` but can be overriden.

As a good practice is to keep a common trait for similar readers, example of common traits: `KafkaReader` or `S3Reader`
Particular implementations could be `AnodotKafkaReader` or `FxbankReader` if specific behaviour needs to be implemented,
otherwise just direct to the `KafkaDefaultReader` which produces a generic `GenericRecord`. See below...

```scala
import ape.kafka.configs.KafkaConfig
import ape.reader.Reader
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Tag, ZIO}

import scala.reflect.ClassTag
//Example of a reader that reads from Kafka

abstract class KafkaReader[E, E1, T :ClassTag] extends Reader[E, E1, T]

protected[kafka] class DefaultReader[Config <: KafkaConfig :Tag]
  extends KafkaReader[Config, Any, ConsumerRecord[String, Array[Byte]]] {
  override protected[this] def read: ZIO[Config, Throwable, ZStream[Any, Throwable, ConsumerRecord[String, Array[Byte]]]] =
    for {
      kafkaConfig <- ZIO.service[Config]
    } yield Consumer
      .subscribeAnd( Subscription.topics(kafkaConfig.topicName) )
      .plainStream(Serde.string, Serde.byteArray)
}
```

In the example above we limit the type `T` to be an instance of a class `ClassTag` and limits the reader to have an 
environment config as a subclass of `KafkaConfig`, please refer to [Config](Configs.md) to see more details of these config 
classes.