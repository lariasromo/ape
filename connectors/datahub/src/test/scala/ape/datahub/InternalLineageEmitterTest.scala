package ape.datahub

import ape.datahub.configs.DatahubConfig
import ape.datahub.models.DummyModels._
import ape.datahub.pipe.{EmitterMechanism, EmitterPipe}
import ape.datahub.reader.EmitterReader
import ape.pipe.Pipe
import ape.reader.Reader
import com.linkedin.common.FabricType
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object InternalLineageEmitterTest extends ZIOSpec[DatahubConfig] {

  val stream = ZStream(Person("a"), Person("b"), Person("c"))

  val reader = EmitterReader(Reader.UnitReader(stream))
  val pipe1 = EmitterPipe.fromPipeBoth(Pipe.UnitTPipe[Any, Any, Person, Car](p => Car(p.name)))
  val pipe2 = EmitterPipe.fromPipeBoth(Pipe.UnitTPipe[Any, Any, Car, House](p => House(p.name)))
  val pipe3 = EmitterPipe.fromPipeBoth(Pipe.UnitTPipe[Any, Any, House, Airplane](p => Airplane(p.name)))
  val pipe4 = EmitterPipe.fromPipeBoth(Pipe.UnitTPipe[Any, Any, Airplane, Building](p => Building(p.name)))
  val pipe5 = EmitterPipe.fromPipeBoth(Pipe.UnitTPipe[Any, Any, Building, Animal](p => Animal(p.name)))

  val pipeline = reader --> (pipe1 --> pipe2 --> pipe3 --> pipe4 --> pipe5)

  override def spec: Spec[DatahubConfig with TestEnvironment with Scope, Any] =
    test("Emit pipeline") {
      for {
        data <- pipeline.runCollect
      } yield {
        assertTrue(data.nonEmpty)
      }
    }

  override def bootstrap: ZLayer[Any, Any, DatahubConfig] = ZLayer.succeed(
    DatahubConfig(
      fabricType = FabricType.PROD,
      mechanism = EmitterMechanism.REST,
      tags = Seq("restTest", "ZIO", "happy", "alexandria"),
      restEmitterUrl = "https://datahub-gms.qa-env.com:443",
      restEmitterToken = ""
    )
  )
}
