# Datahub integration
This integration will allow you to emit datasets directly from a Pipe or Reader input or output types.

### Usage
```scala
case class Person(name:String)

val pipe = Pipe.UnitTPipe[Any, Any, String,   Person](n =>   Person(n))
//using REST emitter
RestEmitterPipe(pipe)
//using Kafka emitter
KafkaEmitterPipe(pipe) 
```

By default only the Left type of a pipe (output type) is going to be emitted, although this can easily be changed.
E.g.
```scala
RestEmitterPipe.fromPipeBoth(pipe)
```

### Config
The emitter uses the following config
```scala
import com.linkedin.common.FabricType

case class DatahubConfig(
                          fabricType: FabricType,
                          restEmitterUrl:String = "http://localhost:8080",
                          restEmitterToken:String = "",
                          schemaRegistryUrl:String = "http://localhost:8080",
                          kafkaBootstrapServers:String = "localhost:9092",
                          tags: Seq[String] = Seq.empty
                        )
```