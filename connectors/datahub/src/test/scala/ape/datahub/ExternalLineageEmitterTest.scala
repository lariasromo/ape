package ape.datahub

import ape.datahub.configs.DatahubConfig
import ape.datahub.models.DummyModels._
import ape.datahub.pipe.{EmitterMechanism, EmitterPipe}
import ape.datahub.reader.EmitterReader
import ape.pipe.Pipe
import ape.reader.Reader
import com.linkedin.common.FabricType
import com.linkedin.common.urn.DatasetUrn
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object ExternalLineageEmitterTest extends ZIOSpec[DatahubConfig] {
  val stream = ZStream(Registration("a"), Registration("b"), Registration("c"))

  val kafkaUrnStr = "urn:li:dataset:(urn:li:dataPlatform:kafka,mcs-attribution,PROD)"
  val kafkaUrn = DatasetUrn.createFromString(kafkaUrnStr)

  val chUrnStr = "urn:li:dataset:(urn:li:dataPlatform:clickhouse,DatabaseNameToBeIngested.datalake.ma_deposit_fxbank,PROD)"
  val chUrn = DatasetUrn.createFromString(chUrnStr)

  val reader = EmitterReader(Reader.UnitReaderStream(stream)).withUpstreams(Seq(kafkaUrn))

  val pipe = EmitterPipe(
    Pipe.UnitTPipe[Any, Any, Registration, RegistrationWithCountry](p => {
      RegistrationWithCountry(p.clientId, Country("UK"))
    }),
    downstream = Some(chUrn)
  )

  val pipeline = reader --> pipe

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
