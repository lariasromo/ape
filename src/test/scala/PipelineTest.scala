import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.{Assert, DefaultRunnableSpec, ZSpec, assertTrue}
import zio.{ZIO, system}


object PipelineTest extends DefaultRunnableSpec {
  val test: ZIO[Clock with Blocking with Any with system.System, Any, Assert] = for {
    testResult <- ZIO.succeed(true)
  } yield {
    assertTrue(testResult)
  }

  override def spec: ZSpec[Environment, Failure] = suite("PipelineTest")(
    testM("Hello world") {
      test
    }
  )
}
