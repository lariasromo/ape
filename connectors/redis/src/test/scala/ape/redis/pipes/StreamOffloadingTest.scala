package ape.redis.pipes

import ape.pipe.Pipe
import ape.reader.Reader
import ape.redis.configs.RedisConfig
import ape.redis.utils.RedisContainerService
import com.redis.testcontainers.RedisContainer
import zio.Console.printLine
import zio.stream.ZStream
import zio.{EnvironmentTag, Scope, ZIO, ZIOApp, ZIOAppArgs, ZLayer, durationInt}

// this is not a unit test but a streaming application that helps showcase offloading aggregations
// which is a very powerful feature of offloading pipelines
object StreamOffloadingTest extends ZIOApp {
  val stream1 = ZStream.iterate(1)(_ + 1)
  //      .schedule(Schedule.spaced(1.millisecond))

  val writer1 = Pipe.UnitZWriter[Any, Any, Int, Int](s =>
    s.groupedWithin(100000, 1.seconds)
      .map(c => c.sum)
      .tap(s => printLine("First sum: " + s))
  )

  val writer2 = Pipe.UnitZWriter[Any, Any, Int, Int](s =>
    s.groupedWithin(100, 10.seconds)
      .map(c => c.sum)
      .tap(s => printLine("Second sum: " + s))
  )

  val pipeWithBackPressure =
    Reader.UnitReader(stream1) --> (writer1 --> ape.misc.Pipes.pipes.backPressure.infinite[Int] --> writer2)
  val pipe =
    Reader.UnitReader(stream1) --> (writer1 --> writer2)

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = for {
    _ <- pipe.runDrain
    //    <&> pipeWithBackPressure.runDrain
  } yield ()

  override implicit def environmentTag: zio.EnvironmentTag[Environment] = EnvironmentTag[Environment]

  override type Environment = RedisContainer with RedisConfig

  override def bootstrap: ZLayer[ZIOAppArgs, Any, Environment] = RedisContainerService.live
}
