ThisBuild / version := "1.3.4"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "pipelineEngine"
  )

val zioVersion = "2.0.10"
val circeVersion = "0.14.3"

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val zioLibraries = Seq(
  "dev.zio" %% "zio"            % zioVersion,
  "dev.zio" %% "zio-concurrent" % zioVersion,
  "dev.zio" %% "zio-kafka" % "2.0.6",
  "dev.zio" %% "zio-s3"    % "0.4.2.3",
  "dev.zio" %% "zio-json"  % "0.4.2",
  "dev.zio" %% "zio-test"     % zioVersion % "test",
  "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
)

libraryDependencies ++= Seq(
  "org.apache.parquet" % "parquet-avro" % "1.12.0",
  "com.softwaremill.sttp.client3" %% "core" % "3.8.0",
  "com.softwaremill.sttp.client3" %% "zio" % "3.8.0",
  "org.apache.avro" % "avro" % "1.10.2",
  "com.google.guava" % "guava" % "11.0.2",
  "org.apache.hadoop" % "hadoop-client" % "2.4.0",
  "org.apache.hadoop" % "hadoop-aws" % "3.3.0",
  "org.apache.hadoop" % "hadoop-common" % "3.3.0",
  "com.amazonaws" % "aws-java-sdk-core" % "1.11.563",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.563",
  "com.sksamuel.avro4s" %% "avro4s-core" % "4.1.0",
  "com.clickhouse" % "clickhouse-jdbc" % "0.3.2-patch11",
  "io.d11"  %% "zhttp"     % "1.0.0.0-RC29",
  "io.github.scottweaver" %% "zio-2-0-testcontainers-kafka" % "0.10.0" % "test",
  "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % "0.10.0" % "test",
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.10" % "test",
  "com.dimafeng" %% "testcontainers-scala-clickhouse" % "0.40.10" % "test",
) ++ zioLibraries ++ circe

// Credentials to get access to Libertex Artifactory maven repositories
credentials += Credentials(Path.userHome / ".sbt" / "artifactory_credentials")

// Libertex Artifactory repository resolver
resolvers += "Artifactory Realm" at s"https://artifactory.fxclub.org/artifactory/alexandria-release"
resolvers += "Artifactory Realm snapshot" at s"https://artifactory.fxclub.org/artifactory/alexandria-snapshot"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _         => MergeStrategy.first
}

// Libertex Artifactory maven repositories (artifact publishing configuration)
publishTo := {
  val artifactory = "https://lbx.jfrog.io/"
  if (isSnapshot.value)
    Some("Artifactory Realm snapshot" at artifactory + "artifactory/alexandria-snapshot")
  else
    Some("Artifactory Realm"  at artifactory + "artifactory/alexandria-release-local")
}