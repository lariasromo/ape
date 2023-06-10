# APE Pipeline Engine

The goal of this project is to generate a common approach when creating new data-consuming services.

An APE pipeline is built on top of the ZIO ZStream interface and allows the user to easily write and read from 
different sources the fastest and easiest possible way.

## Resources
### General descriptions
- [Ape interface](docs/Ape.md)
- [Reader interface](docs/Readers.md)
- [Pipe interface](docs/Pipes.md)
- [Config interface](docs/Configs.md)

### List of available connectors with configs
- [List of Config classes](docs/ConfigList.md)
- [List of Readers](docs/ReaderList.md)
- [List of pipes](docs/PipeList.md)

### Advanced features
- [Pipe operations](docs/PipeOps.md)
- [Read operations](docs/ReaderOps.md)
- [Ape Metrics](docs/Metrics.md)
- [S3 file readers](docs/S3FileReaders.md)
- [Back pressure](docs/BackPressure.md)

# Getting started
- [See example](Readme.md)

### Include APE in your build
```scala
val apeVersion = "3.0.0"

libraryDependencies ++= Seq(
  "com.libertexgroup" %% "ape-core" % apeVersion,
  "com.libertexgroup" %% "ape-cassandra" % apeVersion,
  "com.libertexgroup" %% "ape-clickhouse" % apeVersion,
  "com.libertexgroup" %% "ape-jdbc" % apeVersion,
  "com.libertexgroup" %% "ape-kafka" % apeVersion,
  "com.libertexgroup" %% "ape-redis" % apeVersion,
  "com.libertexgroup" %% "ape-rest" % apeVersion,
  "com.libertexgroup" %% "ape-s3" % apeVersion,
  "com.libertexgroup" %% "ape-websocket" % apeVersion,
)
```