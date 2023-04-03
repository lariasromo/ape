### Scenario
S3 files are stored compressed (gzip) and contains multiple records (as json), one per line (json lines)

Example of records:
```json lines
{"firstName":"Keith", "lastName":"Richards", "country": "AR", "telephone":  "1234567890"}
{"firstName":"Neil", "lastName":"McDonald", "country": "US", "twitter":  "@neilM"}
{"firstName":"Michael", "lastName":"Murphy", "country": "GB"}
{"firstName":"Stephen", "lastName":"Smith", "country": "MX", "address": "1/2 Example Street. State. Country. AB10CD"}
```

This example consists on a pipeline that will read from S3;
1. Reads new files from S3 (using a path pattern) every 5 minutes
2. Opens the file and converts each record to a class. The class may not contain all fields from the file.  
3. Applies a simple filter by country (any country from LATAM)
4. Writes the extracted and filtered records to console, clickhouse and kafka

*** The S3 readers leverage the `S3FileReaderService` to list files from S3 please take a look [at this page](../S3FileReaderService.md) for more details. ***

```scala
import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.readers.s3.{S3FileReaderService, S3FileReaderServiceStream}
import com.libertexgroup.ape.utils.{KafkaUtils, S3Utils}
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig, MultiClickhouseConfig, S3Config}
import com.libertexgroup.models.clickhouse.ClickhouseModel
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.Console.printLine
import zio.kafka.producer.ProducerSettings
import zio.s3.S3
import zio.{Chunk, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.io.IOException
import java.sql.PreparedStatement

object Main extends ZIOAppDefault {
  case class User(
                   firstName: String,
                   lastName: String,
                   country: String
                 ) extends ClickhouseModel {
    override def sql = "INSERT INTO Users(firstName, lastName, country) VALUES(?, ?, ?)"

    override def prepare(preparedStatement: PreparedStatement): Unit = {
      preparedStatement.setString(1, firstName)
      preparedStatement.setString(2, lastName)
      preparedStatement.setString(3, country)
    }
  }

  object User {
    implicit val jsonDecoder: Decoder[User] = deriveDecoder[User]
    implicit val jsonEncoder: Encoder[User] = deriveEncoder[User]
    val toKafka: User => ProducerRecord[String, User] =
      user => new ProducerRecord[String, User]("key", user)
  }

  val latam = Seq("MX", "AR", "CO")

  type Env = S3FileReaderService with S3Config with MultiClickhouseConfig with KafkaConfig with ProducerSettings
  val pipe: ZIO[Env, Throwable, Ape[S3 with S3Config, ((User, Chunk[User]), ProducerRecord[String, User])]] =
    Ape.readers.s3JsonLinesCirceReader[User]
      .mapZ(stream => stream
        .flatMap(s => s._2)
        .filter(r => latam.contains(r.country))
      ) --> (
      Ape.writers.consoleWriter[Any, S3 with S3Config, User] ++
        Ape.writers.clickhouseWriter[S3 with S3Config, User] ++
        Ape.writers.kafkaAvroWriter[S3 with S3Config, User].preMap(User.toKafka)
      )

  val layer = S3Config.live ++
    (ClickhouseConfig.live >+> MultiClickhouseConfig.liveFromNode) ++
    KafkaConfig.live ++ KafkaUtils.liveProducerSettings ++ readerService

  val main = for {
    p <- pipe
    _ <- p.run
  } yield ()

  override def run = main.provideSomeLayer(layer)
}
```