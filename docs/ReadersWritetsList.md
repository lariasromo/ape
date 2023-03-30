## List of Readers (v1)
**Reader description below
- `clickhouseDefaultReader`: Reads from clickhouse database using a sql statement
- `jdbcDefaultReader`: Reads from jdbc supporting database using a sql statement
- `kafkaDefaultReader`: Reads from kafka using a `KafkaConfig`, messages with bytes
- `kafkaAvroReader`: Reads from kafka using a `KafkaConfig` layer, upcoming messages are transformed to objects using a
  case class as reference
- `kafkaJsonCirceReader`: Reads from kafka using a `KafkaConfig` layer, upcoming messages are transformed to objects using a
  case class as reference
- `kafkaStringReader`: Reads from kafka using a `KafkaConfig`, upcoming messages are transformed to objects using a case
  class as reference
- `s3ParquetReader`: Reads parquet files from S3
- `s3AvroReader`: Reads avro datum files from S3
- `s3TextReader`: Reads plaintext from S3
- `S3JsonLinesReader`: Reads text files to S3 that contain a json representation per line, it needs to have an
  implicit conversion `String => T`
- `S3JsonLinesCirceWriter`:Reads text files to S3 that contain a json representation per line, to encode
  object T to json string it uses circe and there has to be an implicit instance of `io.circe.Encoder`
- `websocketReader`: Reads from a websocket (requires a `Websocket[Task]` object) see https://sttp.softwaremill.com/en/latest/examples.html#open-a-websocket-using-zio
-
## List of Writers (v1)
**Writer description below
- `clickhouseWriter`: Writes to a Clickhouse table using `ClickhouseConfig`, which allows writes grouping by batch
  size or time. The input models need to implement the `CLickhouseModel` interface.
- `jDBCWriter`: Writes to a jdbc supporting table using `JDBCConfig`, which allows writes grouping by batch
  size or time. The input models need to implement the `JDBCModel` interface.
- `cassandraWriter`: Writes to a cassandra table using `CassandraConfig`, which allows writes grouping by batch
  size or time. The input models need to implement the `CassandraModel` interface.
- `kafkaStringWriter`: Writes strings to kafka
- `kafkaAvroWriter`: Writes avro bytes to kafka
- `s3AvroWriter`: Writes avro bytes to S3
- `S3JsonLinesWriter`: Writes entities to text files to S3 with a json representation per line, it needs to have an
  implicit conversion `String => T`
- `S3JsonLinesCirceWriter`: Writes entities to text files to S3 with a json representation per line, to encode
  object T to json string it uses circe and there has to be an implicit instance of `io.circe.Encoder`
- `s3ParquetWriter`: Writes parquet files to S3
- `s3TextWriter`: Writes text files to S3
- `consoleWriter`/`consoleStringWriter`: Writes the output to the console (useful for testing purposes)

