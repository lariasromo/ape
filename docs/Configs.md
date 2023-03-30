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
