import zio.test.{Spec, TestEnvironment, TestResult, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}


object PipelineTest extends ZIOSpecDefault {
  val test: ZIO[Any, Nothing, TestResult] = for {
    testResult <- ZIO.succeed(true)
  } yield {
    assertTrue(testResult)
  }

//  override def spec: ZSpec[Environment, Failure] = suite("PipelineTest")(
//    testM("Hello world") {
//      test
//    }
//  )

//
//  override def bootstrap: ZLayer[Any, Any, Nothing] = ZLayer(ZIO.succeed())

  override def spec: Spec[TestEnvironment with Scope, Any] = test("Hello world") {
    test
  }
}
