package ape.datahub.pipe

import ape.datahub.configs.DatahubConfig
import ape.datahub.utils.DatahubUtils._
import ape.pipe.Pipe
import com.linkedin.common.urn.DatasetUrn
import com.sksamuel.avro4s.SchemaFor
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class EmitterPipeLeft[-E, ZE, T1 :SchemaFor :ClassTag, T2 :ClassTag](
      p: Pipe[E, ZE, T1, T2],
      emitterType: EmitterType.Value,
      upstreams:Seq[DatasetUrn] = Seq.empty,
      downstream:Option[DatasetUrn] = None,
      n:Option[String] = None
  ) extends Pipe[E with DatahubConfig, ZE, T1, T2] {


  private def createDatasets: ZIO[DatahubConfig, Throwable, Unit] = for {
    urnLeft <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.LEFT) contains emitterType)(createDataset[T1])
    upstreamLineageResponse <- ZIO.when(upstreams.nonEmpty) {
      createLineage(upstreams, urnLeft.orNull)
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


