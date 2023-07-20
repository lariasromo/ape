import sbt._

object Dependencies {
  val zioVersion = "2.0.10"
  val circeVersion = "0.14.3"
  val droolsVersion = "8.32.0.Final"

  val circeLibraries = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)

  val zioLibraries = Seq(
    "dev.zio" %% "zio-config" % "3.0.7",
    "dev.zio" %% "zio-config-magnolia" % "3.0.7",
    "dev.zio" %% "zio"            % zioVersion,
    "dev.zio" %% "zio-concurrent" % zioVersion,
    "dev.zio" %% "zio-zmx" % "2.0.0-RC4",
    "dev.zio" %% "zio-metrics-connectors" % "2.0.7",
    "dev.zio" %% "zio-http" % "0.0.4",
    "dev.zio" %% "zio-json"  % "0.4.2",
    "dev.zio" %% "zio-test"     % zioVersion % "test",
    "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
  )

  val kafkaLibraries = Seq(
    "dev.zio" %% "zio-kafka" % "2.0.6",
  )

  val s3Libraries = Seq(
    "dev.zio" %% "zio-s3"    % "0.4.2.3",
    "org.apache.parquet" % "parquet-avro" % "1.12.0",
  )

  val redisLibraries = Seq(
    "org.redisson" % "redisson" % "3.20.1",
  )

  val httpLibraries = Seq(
    "com.softwaremill.sttp.client3" %% "core" % "3.8.0",
    "com.softwaremill.sttp.client3" %% "zio" % "3.8.0",
    "com.fxclub.commons" %% "http-trace" % "3.0.0"
  )

  val awsLibraries = Seq(
    "org.apache.hadoop" % "hadoop-client" % "2.4.0",
    "org.apache.hadoop" % "hadoop-aws" % "3.3.0",
    "org.apache.hadoop" % "hadoop-common" % "3.3.0",
    "com.amazonaws" % "aws-java-sdk-core" % "1.11.563",
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.563",
  )

  val clickhouseLibraries = Seq(
    "com.clickhouse" % "clickhouse-jdbc" % "0.3.2-patch11",
  )

  val cassandraLibraries = Seq(
    "io.github.palanga" %% "zio-cassandra" % "0.10.0",
    "com.datastax.cassandra" % "cassandra-driver-core" % "4.0.0" pomOnly()
  )

  val dataHubLibraries = Seq(
    "io.acryl" % "datahub-client" % "0.10.3-1rc1",
    "org.apache.httpcomponents" % "httpasyncclient" % "4.1.5",
    "com.github.java-json-tools" % "json-schema-avro" % "0.1.8"
  )

  val testLibraries = Seq(
    "com.redis.testcontainers" % "testcontainers-redis" % "1.6.4",
    "io.github.scottweaver" %% "zio-2-0-testcontainers-kafka" % "0.10.0" % "test",
    "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % "0.10.0" % "test",
    "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.10" % "test",
    "com.dimafeng" %% "testcontainers-scala-clickhouse" % "0.40.10" % "test",
    "com.dimafeng" %% "testcontainers-scala-cassandra" % "0.40.10" % "test",
    "org.mockito" %% "mockito-scala" % "1.17.12" % "test",
  )

  val commonLibraries = Seq(
    "io.kontainers" %% "purecsv" % "1.3.10",
    "org.apache.avro" % "avro" % "1.10.2",
    "com.google.guava" % "guava" % "11.0.2",
    "com.sksamuel.avro4s" %% "avro4s-core" % "4.1.0",
    "io.d11"  %% "zhttp"     % "1.0.0.0-RC29",
  ) ++ circeLibraries ++ zioLibraries ++ testLibraries
}