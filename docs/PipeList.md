## List of Pipes (v2)

Most pipes receive a type parameter `ET`, this parameter will be used in the `ZStream` it produces.

The only purpose is to match with the incoming stream and concatenate with other `Pipes` or `Readers`. 
If there is no need to match an incoming environment, just set is as `Any`
## Cassandra

Writes to a cassandra table using `CassandraConfig`, which allows writes grouping by batch
size or time. The input models need to implement the `CassandraModel` interface.

```scala
import ape.cassandra.configs.CassandraConfig
import ape.cassandra.models.CassandraModel
import com.datastax.oss.driver.api.core.cql.PreparedStatement

case class Person(name: String) extends CassandraModel {
  override def sql = "INSERT INTO Person(name) VALUES(?);"
  override def bind(preparedStatement:  PreparedStatement) = preparedStatement.bind(name)
}

ape.cassandra.Pipes.pipes[CassandraConfig].default[Any, Person]
```

## Clickhouse
Writes to a Clickhouse table using `ClickhouseConfig`, which allows writes grouping by batch
size or time. The input models need to implement the `CLickhouseModel` interface.
```scala
import ape.clickhouse.configs.MultiClickhouseConfig
import ape.clickhouse.models.ClickhouseModel

import java.sql.PreparedStatement

case class Person(name: String) extends ClickhouseModel {
  override def sql = "INSERT INTO Person(name) VALUES(?);"
  override def prepare(preparedStatement:  PreparedStatement): Unit = preparedStatement.setString(1, name)
}

ape.clickhouse.Pipes.pipes[MultiClickhouseConfig].default[Any, Person]
```

## JDBC
Writes to a jdbc supporting table using `JDBCConfig`, which allows writes grouping by batch size or time. The input 
models need to implement the `JDBCModel` interface.

```scala
import ape.jdbc.configs.JDBCConfig
import ape.jdbc.models.JDBCModel

import java.sql.PreparedStatement

case class Person(name: String) extends JDBCModel {
  override def sql = "INSERT INTO Person(name) VALUES(?);"
  override def prepare(preparedStatement:  PreparedStatement): Unit = preparedStatement.setString(1, name)
} 

ape.jdbc.Pipes.pipes[JDBCConfig].default[Any, Person]
```

## Kafka
Writes content (strings, json, bytes, avro) `JDBCConfig`, allowing grouping by batch size or time

```scala
import ape.kafka.configs.KafkaConfig
case class Person(name: String)

ape.kafka.Pipes.pipes[KafkaConfig].avro.of[Person]
ape.kafka.Pipes.pipes[KafkaConfig].string
```

## Miscellaneous
A set of generic pipes that can be used for testing purposes

```scala
import ape.misc.pipes.QueuePipe
import ape.pipe.Pipe
import zio.ZIO

case class Person(name: String)

val console: Pipe[Any, Any, Person, Person] = 
  ape.misc.Pipes.pipes.console.of[Person]
  
val consoleString: Pipe[Any, Any, String, String] =
  ape.misc.Pipes.pipes.console.ofString

val queuePipe: ZIO[Any, Nothing, QueuePipe[Any, Any, Person]] = for {
  q <- zio.Queue.unbounded[Person]
} yield ape.misc.Pipes.pipes.queue.of[Person](q)
```
## Redis
To access Redis pipes access them from the `generalPurpose` pipes.

Check [this guide](BackPressure.md) that explains `backPressure` pipes.

```scala
import ape.pipe.Pipe
import ape.redis.configs.RedisConfig
case class Person(name: String)

val queue = "queueName"

val string: Pipe[RedisConfig, Any, String, String] = 
  ape.redis.Pipes.pipes[RedisConfig].generalPurpose.string(queue)

val typed: Pipe[RedisConfig, Any, Person, Person] =
  ape.redis.Pipes.pipes[RedisConfig].generalPurpose.typed[Person](queue)
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
import ape.pipe.Pipe
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import zio.http.{Client, Request}

case class Person(name: String)

case class Result(name: String, country: String)

val byte: Pipe[Client, Nothing, Request, Byte] =
  ape.rest.Pipes.pipes.byte

val string: Pipe[Client, Nothing, Request, String] =
  ape.rest.Pipes.pipes.string

val default: Pipe[Client, Any, Request, Person] =
  ape.rest.Pipes.pipes.decode.default[Person]

val circe: Pipe[Client, Any, Request, Person] =
  ape.rest.Pipes.pipes.decode.circe[Person]

val zip: Pipe[Client, Any, Person, (Person, Result)] =
  ape.rest.Pipes.pipes.decode.zip[Person, Result]

val zipCirce: Pipe[Client, Any, Person, (Person, Option[Result])] =
  ape.rest.Pipes.pipes.decode.zipCirce[Person, Result]

val zipCirceWithRequest: Pipe[Client, Any, (Request, Person), (Person, Option[Result])] =
  ape.rest.Pipes.pipes.decode.zipCirceWithRequest[Person, Result] 
```

## S3

```scala
import ape.pipe.Pipe
import ape.s3.configs.S3Config
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

val avro: Pipe[S3 with S3Config, Any, Person, Person] = {
  ape.s3.Pipes.fromData[S3Config].encoded.avro[Person]
}

val text: Pipe[S3 with S3Config, Any, String, String] =
  ape.s3.Pipes.fromData[S3Config].text.default

val csv: Pipe[S3 with S3Config, Any, Person, Person] =
  ape.s3.Pipes.fromData[S3Config].text.csv[Person]()

val jsonLines: Pipe[S3 with S3Config, Any, Person, Person] =
  ape.s3.Pipes.fromData[S3Config].jsonLines.default[Person]

val jsonLinesCirce: Pipe[S3 with S3Config, Any, Person, Person] =
  ape.s3.Pipes.fromData[S3Config].jsonLines.circe[Person]
```