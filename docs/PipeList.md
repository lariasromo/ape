## List of Pipes (v2)

Most pipes receive a type parameter `ET`, this parameter will be used in the `ZStream` it produces.

The only purpose is to match with the incoming stream and concatenate with other `Pipes` or `Readers`. 
If there is no need to match an incoming environment, just set is as `Any`
## Cassandra

Writes to a cassandra table using `CassandraConfig`, which allows writes grouping by batch
size or time. The input models need to implement the `CassandraModel` interface.

```scala
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.CassandraConfig
import com.libertexgroup.models.cassandra.CassandraModel

case class Person(name: String) extends CassandraModel {
  override def sql = "INSERT INTO Person(name) VALUES(?);"
  override def bind(preparedStatement:  PreparedStatement) = preparedStatement.bind(name)
}

Ape.pipes.cassandra[CassandraConfig].default[Any, Person]
```

## Clickhouse
Writes to a Clickhouse table using `ClickhouseConfig`, which allows writes grouping by batch
size or time. The input models need to implement the `CLickhouseModel` interface.
```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.MultiClickhouseConfig
import com.libertexgroup.models.clickhouse.ClickhouseModel

import java.sql.PreparedStatement

case class Person(name: String) extends ClickhouseModel {
  override def sql = "INSERT INTO Person(name) VALUES(?);"
  override def prepare(preparedStatement:  PreparedStatement): Unit = preparedStatement.setString(1, name)
}

Ape.pipes.clickhouse[MultiClickhouseConfig].default[Any, Person]
```

## JDBC
Writes to a jdbc supporting table using `JDBCConfig`, which allows writes grouping by batch size or time. The input 
models need to implement the `JDBCModel` interface.

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.JDBCConfig
import com.libertexgroup.models.jdbc.JDBCModel

import java.sql.PreparedStatement

case class Person(name: String) extends JDBCModel {
  override def sql = "INSERT INTO Person(name) VALUES(?);"
  override def prepare(preparedStatement:  PreparedStatement): Unit = preparedStatement.setString(1, name)
} 

Ape.pipes.jdbc[JDBCConfig].default[Any, Person]
```

## Kafka
Writes content (strings, json, bytes, avro) `JDBCConfig`, allowing grouping by batch size or time

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.KafkaConfig

case class Person(name: String)

Ape.pipes.kafka[KafkaConfig].avro.of[Person]
Ape.pipes.kafka[KafkaConfig].string
```

## Miscellaneous
A set of generic pipes that can be used for testing purposes

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.pipes.misc.QueuePipe
import zio.ZIO

case class Person(name: String)

val console: Pipe[Any, Any, Person, Person] = 
  Ape.pipes.misc.console.of[Person]
  
val consoleString: Pipe[Any, Any, String, String] = 
  Ape.pipes.misc.console.ofString

val queuePipe: ZIO[Any, Nothing, QueuePipe[Any, Any, Person]] = for {
  q <- zio.Queue.unbounded[Person]
} yield Ape.pipes.misc.queue.of[Person](q)
```
## Redis
To access Redis pipes access them from the `generalPurpose` pipes.

Check [this guide](BackPressure.md) that explains `backPressure` pipes.

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs.RedisConfig

case class Person(name: String)

val queue = "queueName"

val string: Pipe[RedisConfig, Any, String, String] =
  Ape.pipes.redis[RedisConfig].generalPurpose.string(queue)

val typed: Pipe[RedisConfig, Any, Person, Person] = 
  Ape.pipes.redis[RedisConfig].generalPurpose.typed[Person](queue)
```
## Rest
This connector type has several pipe creation options.

### Simple
- **byte**: This pipe receives an input type `Request` which is used to call an external API and gives back the
  raw response body in bytes.
- **string**: This pipe receives an input type `Request` which is used to call an external API and gives back the
  raw response body in string.
### Decoding
- **default[T]**: This pipe receives an input type `Request` which is used to call an external API and decodes
  the json response using an implicit conversion `String => T`. The result type is a pair of `T`.
- **circe[T]**: This pipe receives an input type `Request` which is used to call an external API and decodes
  the json response using circe. The result type is of type `T`.
- **zip[T, T2]**: This pipe receives an input type `T` which is converted to a Request object using an implicit
  conversion `T => Request`, such Request is used to call an external API and decodes
  the json response using an implicit conversion `String => T`. The result type is a pair of `(T, T2)`.
- **zipCirce[T, T2]**: This pipe receives an input type `T` which is converted to a Request object using an implicit
  conversion `T => Request`, such Request is used to call an external API and decodes the json response using circe.
  The result type is a pair of `(T, Option[T2])`.
- **zipCirceWithRequest[T, T2]**: This pipe receives an input type `(Rerquest, T)` which means that the Request
  object needs to be built on the client side, such Request is used to call an external API and decodes the json
  response using circe. The result type is a pair of `(T, Option[T2])`.

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import zio.http.{Client, Request}

case class Person(name: String)
case class Result(name: String, country: String)

val byte: Pipe[Client, Nothing, Request, Byte] =
  Ape.pipes.rest.byte

val string: Pipe[Client, Nothing, Request, String] =
  Ape.pipes.rest.string
  
val default: Pipe[Client, Any, Request, Person] =
  Ape.pipes.rest.decode.default[Person]

val circe: Pipe[Client, Any, Request, Person] =
  Ape.pipes.rest.decode.circe[Person]

val zip: Pipe[Client, Any, Person, (Person, Result)] =
  Ape.pipes.rest.decode.zip[Person, Result]

val zipCirce: Pipe[Client, Any, Person, (Person, Option[Result])] =
  Ape.pipes.rest.decode.zipCirce[Person, Result]

val zipCirceWithRequest: Pipe[Client, Any, (Request, Person), (Person, Option[Result])] =
  Ape.pipes.rest.decode.zipCirceWithRequest[Person, Result] 
```

## S3

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs.S3Config
import zio.s3.S3
import io.circe._
import io.circe.generic.semiauto.deriveEncoder

case class Person(name: String)
object Person{
  // if writing to json lines use either one of these encoders
  // use with circe encoder
  implicit val jsonEncoder: Encoder[Person] = deriveEncoder[Person]
  // use with default encoder
  implicit val strEncoder: Person => String = p => p.name
}

val avro: Pipe[S3 with S3Config, Any, Person, Person] =
  Ape.pipes.s3[S3Config].encoded.avro[Person]

val text: Pipe[S3 with S3Config, Any, String, String] =
  Ape.pipes.s3[S3Config].text.default

val csv: Pipe[S3 with S3Config, Any, Person, Person] =
  Ape.pipes.s3[S3Config].text.csv[Person]()

val jsonLines: Pipe[S3 with S3Config, Any, Person, Person] =
  Ape.pipes.s3[S3Config].jsonLines.default[Person]

val jsonLinesCirce: Pipe[S3 with S3Config, Any, Person, Person] = 
  Ape.pipes.s3[S3Config].jsonLines.circe[Person]
```