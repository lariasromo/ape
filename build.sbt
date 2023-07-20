import Dependencies._

ThisBuild / version := "3.4"
ThisBuild / scalaVersion := "2.13.10"

lazy val commonSettings = Seq(
  organization := "com.libertexgroup",
  resolvers := res
)

lazy val cassandra = (project in file("connectors/cassandra"))
  .settings(commonSettings,
    name := "ape-cassandra",
    libraryDependencies ++= commonLibraries ++ cassandraLibraries)
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val clickhouse = (project in file("connectors/clickhouse"))
  .settings(commonSettings,
    name := "ape-clickhouse",
    libraryDependencies ++= commonLibraries ++ clickhouseLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val datahub = (project in file("connectors/datahub"))
  .settings(commonSettings,
    name := "ape-datahub",
    libraryDependencies ++= commonLibraries ++ dataHubLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val jdbc = (project in file("connectors/jdbc"))
  .settings(commonSettings,
    name := "ape-jdbc",
    libraryDependencies ++= commonLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val kafka = (project in file("connectors/kafka"))
  .settings(commonSettings,
    name := "ape-kafka",
    libraryDependencies ++= commonLibraries ++ kafkaLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val redis = (project in file("connectors/redis"))
  .settings(commonSettings,
    name := "ape-redis",
    libraryDependencies ++= commonLibraries ++ redisLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val rest = (project in file("connectors/rest"))
  .settings(commonSettings,
    name := "ape-rest",
    libraryDependencies ++= commonLibraries ++ httpLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val s3 = (project in file("connectors/s3"))
  .settings(commonSettings,
    name := "ape-s3",
    libraryDependencies ++= commonLibraries ++ awsLibraries ++ s3Libraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core, redis)

lazy val websocket = (project in file("connectors/websocket"))
  .settings( commonSettings,
    name := "ape-websocket",
    libraryDependencies ++= commonLibraries ++ httpLibraries ++ testLibraries)
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val examples = (project in file("examples"))
  .settings(commonSettings,
    publish / skip := true,
    libraryDependencies ++= cassandraLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(cassandra, clickhouse, jdbc, kafka, redis, rest, s3, websocket, core)

lazy val core = (project in file("core"))
  .settings(
    name := "ape-core",
    commonSettings,
    libraryDependencies ++= commonLibraries ++ kafkaLibraries
  )
  .enablePlugins(JavaAppPackaging)

//lazy val ape = (project in file("."))
//  .aggregate(core)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _         => MergeStrategy.first
}

// Libertex Artifactory maven repositories (artifact publishing configuration)
ThisBuild / publishTo := {
  val artifactory = "https://lbx.jfrog.io/"
  if (isSnapshot.value)
  Some("Artifactory Realm snapshot" at artifactory + "artifactory/alexandria-snapshot")
  else
  Some("Artifactory Realm"  at artifactory + "artifactory/alexandria-release")
}

Test / parallelExecution := false

// Credentials to get access to Libertex Artifactory maven repositories
credentials += Credentials(Path.userHome / ".sbt" / "artifactory_credentials")

// Libertex Artifactory repository resolver
val res = Seq(
  "Artifactory Realm" at s"https://lbx.jfrog.io/artifactory/alexandria-release",
  "Artifactory Realm snapshot" at s"https://lbx.jfrog.io/artifactory/alexandria-snapshot",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
