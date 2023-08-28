package ape.datahub

import ape.datahub.configs.DatahubConfig
import ape.datahub.models.DummyModels._
import ape.datahub.pipe.{EmitterMechanism, EmitterPipe}
import ape.pipe.Pipe
import ape.reader.Reader
import com.linkedin.common.FabricType
import com.linkedin.common.urn.DatasetUrn
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object RestEmitterTest extends ZIOSpec[DatahubConfig] {

  val stream = ZStream("a", "b", "c").map(n => Person(n))
  val externalUrn = DatasetUrn.createFromString("urn:li:dataset:(urn:li:dataPlatform:kafka,mcs-attribution,PROD)")
  val reader = Reader.UnitReader(stream)
  val pipe1 = EmitterPipe(Pipe.UnitTPipe[Any, Any, Person,   Car](p =>      Car(p.name)))
  val pipe2 = EmitterPipe(Pipe.UnitTPipe[Any, Any, Car,      House](p =>    House(p.name)),
    upstreams = Seq(externalUrn)
  )
  val pipe3 = EmitterPipe(Pipe.UnitTPipe[Any, Any, House,    Airplane](p => Airplane(p.name)))
  val pipe4 = EmitterPipe(Pipe.UnitTPipe[Any, Any, Airplane, Building](p => Building(p.name)))
  val pipe5 = EmitterPipe(Pipe.UnitTPipe[Any, Any, Building, Animal](p =>   Animal(p.name)))

  val pipeline = reader --> (pipe1 --> pipe2 --> pipe3 --> pipe4 --> pipe5)

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
      mechanism = EmitterMechanism.REST,
      tags = Seq.empty,
      restEmitterUrl = "https://datahub-gms.qa-env.com:443",
      restEmitterToken = "eyJhbGciOiJIUzI1NiJ9.eyJhY3RvclR5cGUiOiJVU0VSIiwiYWN0b3JJZCI6ImRhdGFodWIiLCJ0eXBlIjoiUEVSU09OQUwiLCJ2ZXJzaW9uIjoiMiIsImp0aSI6ImM1ZmVlMzFkLWU0MDQtNDQ2Ny1hMzRkLWZmN2I0MDJiMjA3OCIsInN1YiI6ImRhdGFodWIiLCJpc3MiOiJkYXRhaHViLW1ldGFkYXRhLXNlcnZpY2UifQ.GOCMRa6KujhQvR6WtUD1eeRvyBe6ojzCfN9TYnGvMmc"
    )
  )
}