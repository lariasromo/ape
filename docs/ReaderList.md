## List of Readers (v2)

## Cassandra
Reads from clickhouse database using a sql statement

### Example
```scala
import ape.cassandra.configs.CassandraConfig
import ape.reader.Reader
import com.datastax.oss.driver.api.core.cql.Row

case class Person(name: String) 
object Person {
  implicit val cql: Row => Person = rs => Person(rs.getString("name"))
}

val cassandra: Reader[CassandraConfig, Any, Person] =
  ape.cassandra.Readers.readers[CassandraConfig].default[Person]("select * from Person")
```

## Clickhouse
Reads from clickhouse database using a sql statement

### Example
```scala
import ape.clickhouse.configs.MultiClickhouseConfig
import ape.reader.Reader
import com.datastax.oss.driver.api.core.cql.Row

import java.sql.ResultSet

case class Person(name: String)
object Person {
  implicit val sql: ResultSet => Person = rs => Person(rs.getString("name"))
}

val clickhouse: Reader[MultiClickhouseConfig, Any, Person] = { 
  ape.clickhouse.Readers.readers[MultiClickhouseConfig].default[Person]("select * from Person")
}
```

## Jdbc
Reads from jdbc supporting database using a sql statement

### Example
```scala

import ape.jdbc.configs.JDBCConfig
import ape.reader.Reader

import java.sql.ResultSet

case class Person(name: String) 
object Person {
  implicit val sql: ResultSet => Person = rs => Person(rs.getString("name"))
}

val jdbc: Reader[JDBCConfig, Any, Person] = { 
  ape.jdbc.Readers.readers[JDBCConfig].default[Person]("select * from Person")
}
```

## Kafka
Reads from kafka using a `KafkaConfig`, the result type could be messages in bytes, strings or converted directly to 
case classes

### Example

```scala
import ape.kafka.configs.KafkaConfig
import ape.reader.Reader
import org.apache.kafka.clients.consumer.ConsumerRecord

case class Person(name: String)

val avro: Reader[KafkaConfig, Any, ConsumerRecord[String, Option[Person]]] = 
  ape.kafka.Readers.readers[KafkaConfig].avro[Person]

val bytes: Reader[KafkaConfig, Any, ConsumerRecord[String, Array[Byte]]] =
  ape.kafka.Readers.readers[KafkaConfig].default

val string: Reader[KafkaConfig, Any, ConsumerRecord[String, String]] =
  ape.kafka.Readers.readers[KafkaConfig].string

val circe: Reader[KafkaConfig, Any, ConsumerRecord[String, Person]] =
  ape.kafka.Readers.readers[KafkaConfig].jsonCirce[Person]
```

# Redis 
Writes to a redis database queue using a queue name and a limit or messages to read. If the limit is negative it 
will not limit to a number of records creating an infinite stream.
### Example

```scala
import ape.reader.Reader
import ape.redis.configs.RedisConfig
case class Person(name: String)

val avro: Reader[RedisConfig, Any, Person] =
  ape.redis.Readers.readers[RedisConfig].avro[Person]("queueName", -1)
  
val string: Reader[RedisConfig, Any, String] =
  ape.redis.Readers.readers[RedisConfig].string("queueName", -1)
```

# Rest
Will read from a Rest endpoint using a `zio.http.Request` entity. An http reader will just call an endpoint once, 
this could be useful if the response can be flattened to multiple records.

```scala
import ape.reader.Reader
import zio.http.{Body, Client, Request, URL}

val request = Request.post(
  Body.fromString("""{"prop": "value"}"""), 
  URL.fromString("http://example.com")
)

val byte: Reader[Client, Any, Byte] =
  ape.rest.Readers.readers.byte(request)
  
val string: Reader[Client, Any, String] =
  ape.rest.Readers.readers.string(request)
```

