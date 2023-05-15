package examples.pipes

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.{ClickhouseConfig, KafkaConfig, MultiClickhouseConfig, S3Config}
import com.libertexgroup.utils.S3Utils
import examples.models.User
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Chunk, Scope, ZIO, ZIOApp, ZIOAppArgs, ZLayer}

object Example2 extends ZIOApp {
  val latam = Seq("MX", "AR", "CO")
  val location = "someS3Location"

  val reader: Reader[Any, S3Config, User] =
    Ape.readers.s3[S3Config]
      .fileReaderContinuous(S3Utils.pathConverter(location))
      .readFiles
      .jsonLinesCirce[User]
      .mapZ(stream => stream
        .flatMap(s => s._2)
        .filter(r => latam.contains(r.country))
      )

  val consolePipe: Pipe[Any, S3Config, User, User] =
    Ape.pipes.misc
      .console[Any, S3Config].of[User]

  val clickhousePipe: Pipe[MultiClickhouseConfig, S3Config, User, Chunk[(User, Int)]] =
    Ape.pipes
      .clickhouse[MultiClickhouseConfig]
      .default[S3Config, User]

  val kafkaPipe: Pipe[KafkaConfig, S3Config, User, ProducerRecord[String, User]] =
    Ape.pipes.kafka[KafkaConfig]
      .avro[S3Config]
      .of[User]
      .contramap(User.toKafka) // we need to convert User into a kafka record before sending it to the Pipe

  val allPipesParallel: Pipe[MultiClickhouseConfig with S3Config with Scope with KafkaConfig, S3Config, User, Unit] =
    (consolePipe ++ clickhousePipe ++ kafkaPipe)
      .map(_ => ())  // the result of the pipeline will still have a result
  // but since we wont pass it to any other pipe its safe to discard it

  val pipe = reader --> allPipesParallel

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Throwable, Unit] = pipe.runDrain

  override implicit def environmentTag: zio.EnvironmentTag[Environment] = zio.EnvironmentTag[Environment]

  override type Environment = S3Config with MultiClickhouseConfig with KafkaConfig

  override def bootstrap: ZLayer[ZIOAppArgs, Any, Environment] = S3Config.live() ++
    (ClickhouseConfig.live() >+> MultiClickhouseConfig.liveFromNode) ++ KafkaConfig.live()
}