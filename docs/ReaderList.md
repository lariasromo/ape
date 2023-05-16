## List of Readers (v2)

## Cassandra
Reads from clickhouse database using a sql statement

### Example
```scala
import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.CassandraConfig

case class Person(name: String) 
object Person {
  implicit val cql: Row => Person = rs => Person(rs.getString("name"))
}

val cassandra: Reader[CassandraConfig, Any, Person] = 
  Ape.readers.cassandra[CassandraConfig].default[Person]("select * from Person")
```

## Clickhouse
Reads from clickhouse database using a sql statement

### Example
```scala
import com.datastax.oss.driver.api.core.cql.Row
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.MultiClickhouseConfig

import java.sql.ResultSet

case class Person(name: String)
object Person {
  implicit val sql: ResultSet => Person = rs => Person(rs.getString("name"))
}

val clickhouse: Reader[MultiClickhouseConfig, Any, Person] = 
  Ape.readers.clickhouse[MultiClickhouseConfig].default[Person]("select * from Person")
```

## Jdbc
Reads from jdbc supporting database using a sql statement

### Example
```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.JDBCConfig

import java.sql.ResultSet

case class Person(name: String) 
object Person {
  implicit val sql: ResultSet => Person = rs => Person(rs.getString("name"))
}

val jdbc: Reader[JDBCConfig, Any, Person] = 
  Ape.readers.jdbc[JDBCConfig].default[Person]("select * from Person")
```

## Kafka
Reads from kafka using a `KafkaConfig`, the result type could be messages in bytes, strings or converted directly to 
case classes

### Example

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord

case class Person(name: String)

val avro: Reader[KafkaConfig, Any, ConsumerRecord[String, Option[Person]]] = 
  Ape.readers.kafka[KafkaConfig].avro[Person]

val bytes: Reader[KafkaConfig, Any, ConsumerRecord[String, Array[Byte]]] = 
  Ape.readers.kafka[KafkaConfig].default

val string: Reader[KafkaConfig, Any, ConsumerRecord[String, String]] = 
  Ape.readers.kafka[KafkaConfig].string

val circe: Reader[KafkaConfig, Any, ConsumerRecord[String, Person]] = 
  Ape.readers.kafka[KafkaConfig].jsonCirce[Person]
```

# Redis 
Writes to a redis database queue using a queue name and a limit or messages to read. If the limit is negative it 
will not limit to a number of records creating an infinite stream.
### Example

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.RedisConfig

case class Person(name: String)

val avro: Reader[RedisConfig, Any, Person] = 
  Ape.readers.redis[RedisConfig].avro[Person]("queueName", -1)
  
val string: Reader[RedisConfig, Any, String] = 
  Ape.readers.redis[RedisConfig].string("queueName", -1)
```

# Rest
Will read from a Rest endpoint using a `zio.http.Request` entity. An http reader will just call an endpoint once, 
this could be useful if the response can be flattened to multiple records.

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import zio.http.{Body, Client, Request, URL}

val request = Request.post(
  Body.fromString("""{"prop": "value"}"""), 
  URL.fromString("http://example.com")
)

val byte: Reader[Client, Any, Byte] = 
  Ape.readers.rest.byte(request)
  
val string: Reader[Client, Any, String] = 
  Ape.readers.rest.string(request)
```

