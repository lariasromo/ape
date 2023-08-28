package ape.datahub.pipe

import ape.datahub.configs.DatahubConfig
import ape.datahub.utils.DatahubUtils
import ape.datahub.utils.DatahubUtils._
import ape.pipe.Pipe
import com.linkedin.common.urn.DatasetUrn
import com.sksamuel.avro4s.SchemaFor
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class EmitterPipeBoth[-E, ZE, T1 : SchemaFor :ClassTag, T2 : SchemaFor :ClassTag](
      p: Pipe[E, ZE, T1, T2],
      emitterType: EmitterType.Value,
      upstreams:Seq[DatasetUrn] = Seq.empty,
      downstream:Option[DatasetUrn] = None,
      n:Option[String] = None
  ) extends Pipe[E with DatahubConfig, ZE, T1, T2] {


  private def createDatasets: ZIO[DatahubConfig, Throwable, Unit] = for {
    urnLeft <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.LEFT) contains emitterType)(DatahubUtils.createDataset[T1])
    urnRight <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.RIGHT) contains emitterType)(createDataset[T2])
    leftRightLineageResp <- ZIO.when(emitterType.equals(EmitterType.BOTH) && urnLeft.isDefined && urnRight.isDefined) {
      createLineage(Seq(urnLeft.orNull), urnRight.orNull)
    }
    upstreamLineageResponse <- ZIO.when(upstreams.nonEmpty) {
      createLineage(upstreams, urnLeft.orNull)
    }
    downstreamLineageResponse <- ZIO.when(downstream.isDefined) {
      createLineage(Seq(urnRight.orNull), downstream.orNull)
    }
  } yield ()

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T1]):
    ZIO[E with DatahubConfig, Throwable, ZStream[ZE, Throwable, T2]] =
      for {
        _ <- createDatasets
        s <- p.apply(i)
      } yield s

  override def name: String = n.getOrElse(p.name)
}

