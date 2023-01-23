ThisBuild / version := "0.1.2"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "pipelineEngine"
  )

libraryDependencies ++= Seq(
//  "com.libertexgroup"   %% "schemas" % "0.1.4",
  "org.apache.parquet" % "parquet-avro" % "1.12.0",
  "org.apache.avro" % "avro" % "1.10.2",
  "com.google.guava" % "guava" % "11.0.2",
  "org.apache.hadoop" % "hadoop-client" % "2.4.0",
  "org.apache.hadoop" % "hadoop-aws" % "3.3.0",
  "org.apache.hadoop" % "hadoop-common" % "3.3.0",
  "com.amazonaws" % "aws-java-sdk-core" % "1.11.563",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.563",
  "com.sksamuel.avro4s" %% "avro4s-core" % "4.1.0",
  "com.clickhouse" % "clickhouse-jdbc" % "0.3.2-patch11",
  "dev.zio" %% "zio"       % "1.0.16",
  "io.d11"  %% "zhttp"     % "1.0.0.0-RC29",
  "dev.zio" %% "zio-kafka" % "0.15.0",
  "dev.zio" %% "zio-s3"    % "0.3.7",
  "dev.zio" %% "zio-json"  % "0.1.5",
  "dev.zio" %% "zio-test"     % "1.0.16" % "test",
  "dev.zio" %% "zio-test-sbt" % "1.0.16" % "test",
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.10" % "test",
  "com.dimafeng" %% "testcontainers-scala-clickhouse" % "0.40.10" % "test",
  "com.dimafeng" %% "testcontainers-scala-kafka" % "0.40.10" % "test",
)

// Credentials to get access to Libertex Artifactory maven repositories
credentials += Credentials(Path.userHome / ".sbt" / "artifactory_credentials")

// Libertex Artifactory repository resolver
resolvers += "Artifactory Realm" at s"https://artifactory.fxclub.org/artifactory/alexandria-release"
resolvers += "Artifactory Realm snapshot" at s"https://artifactory.fxclub.org/artifactory/alexandria-snapshot"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)


// Libertex Artifactory maven repositories (artifact publishing configuration)
publishTo := {
  val artifactory = "https://lbx.jfrog.io/"
  if (isSnapshot.value)
    Some("Artifactory Realm snapshot" at artifactory + "artifactory/alexandria-snapshot")
  else
    Some("Artifactory Realm"  at artifactory + "artifactory/alexandria-release-local")
}