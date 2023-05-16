## Cassandra

```scala
case class CassandraConfig(
                host: String,
                keyspace: String,
                port: Int,
                batchSize: Int,
                syncDuration: zio.Duration,
                username: String,
                password: String,
                datacenter: String="datacenter1",
              )
```

## Clickhouse
This config file consists on a set of clickhouse nodes that constitute a cluster.

```scala
case class ClickhouseConfig(
                             batchSize: Int,
                             syncDuration: Duration,
                             host: String,
                             port: Int,
                             databaseName: String,
                             username: String,
                             password: String,
                             clusterName: Option[String],
                             socketTimeout: Duration = 3.minutes
                           )
case class MultiClickhouseConfig(chConfigs: List[ClickhouseConfig])
```


## Jdbc

```scala
case class JDBCConfig(
                       batchSize: Int,
                       syncDuration: Duration,
                       driverName:String,
                       jdbcUrl: String,
                       username: String,
                       password: String,
                       socketTimeout: Duration = 3.minutes
                       )
```

## Kafka

```scala
case class KafkaConfig(
                        topicName: String,
                        kafkaBrokers: List[String],
                        consumerGroup: String,
                        clientId: String,
                        flushSeconds: Duration,
                        batchSize: Int,
                        autoOffsetStrategy: AutoOffsetStrategy,
                        additionalProperties: Map[String, String]
  )
```

# Redis

```scala
import org.redisson.config.Config
case class RedisConfig(config:Config)
```