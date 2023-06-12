package ape.datahub

import ape.datahub.configs.DatahubConfig
import ape.datahub.models.DummyModels.{Animal, Person, _}
import ape.datahub.pipe.RestEmitterPipe
import ape.pipe.Pipe
import ape.reader.Reader
import com.linkedin.common.FabricType
import zio.Console.printLine
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer}

object RestEmitterTest extends ZIOSpec[DatahubConfig] {
  val stream: ZStream[Any, Nothing, String] = ZStream("a", "b", "c")

  val reader:       Reader[Any, Any, String] = Reader.UnitReader(stream)
  val PersonPipe:   Pipe[Any, Any, String,   Person] = Pipe.UnitTPipe(n =>   Person(n))
  val CarPipe:      Pipe[Any, Any, Person,   Car] = Pipe.UnitTPipe(p =>      Car(p.name))
  val HousePipe:    Pipe[Any, Any, Car,      House] = Pipe.UnitTPipe(p =>    House(p.name))
  val AirplanePipe: Pipe[Any, Any, House,    Airplane] = Pipe.UnitTPipe(p => Airplane(p.name))
  val BuildingPipe: Pipe[Any, Any, Airplane, Building] = Pipe.UnitTPipe(p => Building(p.name))
  val AnimalPipe:   Pipe[Any, Any, Building, Animal] = Pipe.UnitTPipe(p =>   Animal(p.name))

  val pipe0 = RestEmitterPipe(PersonPipe)
  val pipe1 = RestEmitterPipe(CarPipe)
  val pipe2 = RestEmitterPipe(HousePipe)
  val pipe3 = RestEmitterPipe(AirplanePipe)
  val pipe4 = RestEmitterPipe(BuildingPipe)
  val pipe5 = RestEmitterPipe(AnimalPipe)

  val pipeline = reader --> (pipe0 --> pipe1 --> pipe2 --> pipe3 --> pipe4 --> pipe5)

  override def spec: Spec[DatahubConfig with TestEnvironment with Scope, Any] =
    test("Emit pipeline"){
      for {
        data <- pipeline.runCollect
        _ <- ZIO.foreach(data)(printLine(_))
      } yield {
        assertTrue(data.nonEmpty)
      }
    }

  override def bootstrap: ZLayer[Any, Any, DatahubConfig] = ZLayer.succeed(
    DatahubConfig(
      fabricType = FabricType.DEV,
      tags = Seq("restTest", "ZIO", "happy", "alexandria"),
      restEmitterUrl = "https://datahub-gms.qa-env.com:443",
      restEmitterToken = ""
    )
  )
}