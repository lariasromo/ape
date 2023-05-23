# [Configs](src/main/scala/com/libertexgroup/configs)
 
 - [Config List](ConfigList.md) 

We can keep reusable configuration case classes associated to a source or target e.g. `JDBCConfig`, `S3Config`, `ClickhouseConfig` etc.


Example of a config to connect to a source or target (kafka)
```scala
import zio.Duration
case class KafkaConfig(
                        topicName: String,
                        kafkaBrokers: List[String],
                        consumerGroup: String,
                        flushSeconds: Duration,
                        batchSize: Int
                      )
```
This project also includes `live` methods that creates an instance of these config classes using environment variables as sources.

**Same config type for two different sources/targets**

In some cases we will need to read and write from different location using the same config type e.g. reading from a kafka broker and writing records to a different broker.
Although when we build our pipeline we will have a `ZStream` with two instances of the same config type.

```scala
import ape.kafka.configs.KafkaConfig
import zio.Duration

class Kafka1(
              topicName: String,
              kafkaBrokers: List[String],
              consumerGroup: String,
              flushSeconds: Duration,
              batchSize: Int
) extends KafkaConfig(
  topicName, 
  kafkaBrokers, 
  consumerGroup, 
  flushSeconds, 
  batchSize
)
```
```scala
import ape.kafka.configs.KafkaConfig
import zio.Duration

class Kafka2(
              topicName: String,
              kafkaBrokers: List[String],
              consumerGroup: String,
              flushSeconds: Duration,
              batchSize: Int
) extends KafkaConfig(
  topicName, 
  kafkaBrokers, 
  consumerGroup, 
  flushSeconds, 
  batchSize
)
```

## Example
Read and write to kafka using the same config type with different values each.
```scala

ape.kafka.Readers.readers[Kafka1].string --> 
  ape.kafka.Pipes.pipes[Kafka2].string
```