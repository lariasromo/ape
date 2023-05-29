package ape.datahub

import ape.datahub.configs.DatahubConfig
import ape.datahub.models.DummyModels._
import ape.datahub.pipe.KafkaEmitterPipe
import ape.pipe.Pipe
import ape.reader.Reader
import com.linkedin.common.FabricType
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object KafkaEmitterTest extends ZIOSpec[DatahubConfig] {
  val stream = ZStream("a", "b", "c")

  val reader = Reader.UnitReader(stream)
  val pipe0 = KafkaEmitterPipe(Pipe.UnitTPipe[Any, Any, String,   Person](n =>   Person(n)))
  val pipe1 = KafkaEmitterPipe(Pipe.UnitTPipe[Any, Any, Person,   Car](p =>      Car(p.name)))
  val pipe2 = KafkaEmitterPipe(Pipe.UnitTPipe[Any, Any, Car,      House](p =>    House(p.name)))
  val pipe3 = KafkaEmitterPipe(Pipe.UnitTPipe[Any, Any, House,    Airplane](p => Airplane(p.name)))
  val pipe4 = KafkaEmitterPipe(Pipe.UnitTPipe[Any, Any, Airplane, Building](p => Building(p.name)))
  val pipe5 = KafkaEmitterPipe(Pipe.UnitTPipe[Any, Any, Building, Animal](p =>   Animal(p.name)))

  val pipeline = reader --> (pipe0 --> pipe1 --> pipe2 --> pipe3 --> pipe4 --> pipe5)

  override def spec: Spec[DatahubConfig with TestEnvironment with Scope, Any] =
    test("Emit pipeline"){
      for {
        data <- pipeline.runCollect
      } yield {
        assertTrue(data.nonEmpty)
      }
    }

  override def bootstrap: ZLayer[Any, Any, DatahubConfig] = ZLayer.succeed(DatahubConfig(fabricType = FabricType.DEV))
}