import Dependencies._

ThisBuild / version := "3.0.0"
ThisBuild / scalaVersion := "2.13.10"

lazy val cassandra = (project in file("connectors/cassandra"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries ++ cassandraLibraries)
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core, misc)

lazy val clickhouse = (project in file("connectors/clickhouse"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries ++ clickhouseLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val jdbc = (project in file("connectors/jdbc"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val kafka = (project in file("connectors/kafka"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries ++ kafkaLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val misc = (project in file("connectors/misc"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val redis = (project in file("connectors/redis"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries ++ redisLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core, misc)

lazy val rest = (project in file("connectors/rest"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries ++ httpLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)

lazy val s3 = (project in file("connectors/s3"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries ++ awsLibraries ++ s3Libraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core, redis, misc)

lazy val websocket = (project in file("connectors/websocket"))
  .settings(commonSettings,
    libraryDependencies ++= commonLibraries ++ httpLibraries ++ testLibraries)
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core)


lazy val examples = (project in file("examples"))
  .settings(commonSettings,
    libraryDependencies ++= cassandraLibraries
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(cassandra, clickhouse, jdbc, kafka, misc, redis, rest, s3, websocket, core)


lazy val commonSettings = Seq(
  name := "pipelineEngine",
  organization := "com.libertexgroup",
  //  libraryDependencies ++= deps,
  resolvers := res
)

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    libraryDependencies ++= commonLibraries ++ kafkaLibraries
  )
  .enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
  .aggregate(core)

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
