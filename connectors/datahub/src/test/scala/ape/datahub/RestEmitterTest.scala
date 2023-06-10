package ape.datahub

import ape.datahub.configs.DatahubConfig
import ape.datahub.models.DummyModels._
import ape.datahub.pipe.RestEmitterPipe
import ape.pipe.Pipe
import ape.reader.Reader
import com.linkedin.common.FabricType
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object RestEmitterTest extends ZIOSpec[DatahubConfig] {

  val stream = ZStream("a", "b", "c")

  val reader = Reader.UnitReader(stream)
  val pipe0 = RestEmitterPipe(Pipe.UnitTPipe[Any, Any, String,   Person](n =>   Person(n)))
  val pipe1 = RestEmitterPipe(Pipe.UnitTPipe[Any, Any, Person,   Car](p =>      Car(p.name)))
  val pipe2 = RestEmitterPipe(Pipe.UnitTPipe[Any, Any, Car,      House](p =>    House(p.name)))
  val pipe3 = RestEmitterPipe(Pipe.UnitTPipe[Any, Any, House,    Airplane](p => Airplane(p.name)))
  val pipe4 = RestEmitterPipe(Pipe.UnitTPipe[Any, Any, Airplane, Building](p => Building(p.name)))
  val pipe5 = RestEmitterPipe(Pipe.UnitTPipe[Any, Any, Building, Animal](p =>   Animal(p.name)))

  val pipeline = reader --> (pipe0 --> pipe1 --> pipe2 --> pipe3 --> pipe4 --> pipe5)

  override def spec: Spec[DatahubConfig with TestEnvironment with Scope, Any] =
    test("Emit pipeline"){
      for {
        data <- pipeline.runCollect
      } yield {
        assertTrue(data.nonEmpty)
      }
    }

  override def bootstrap: ZLayer[Any, Any, DatahubConfig] = ZLayer.succeed(
    DatahubConfig(
      fabricType = FabricType.DEV,
      tags = Seq("restTest", "ZIO", "happy", "alexandria")
//      restEmitterUrl = "https://datahub.qa-env.com",
//      restEmitterToken = "eyJhbGciOiJIUzI1NiJ9.eyJhY3RvclR5cGUiOiJVU0VSIiwiYWN0b3JJZCI6ImRhdGFodWJfZ3Vlc3QiLCJ0eXBlIjoiUEVSU09OQUwiLCJ2ZXJzaW9uIjoiMiIsImp0aSI6ImQzZjUzZTA0LTJhZjYtNGNkMi04MWJiLTUxMjAyYzc3MTBmYiIsInN1YiI6ImRhdGFodWJfZ3Vlc3QiLCJleHAiOjE2ODg5MjgyODEsImlzcyI6ImRhdGFodWItbWV0YWRhdGEtc2VydmljZSJ9.quUz-0I8UgNTypp_WhFbaMkLDBPx-dAhUCwy5MxDWM8"
    )
  )
}