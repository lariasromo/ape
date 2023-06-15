package ape.datahub.reader

import ape.datahub.configs.DatahubConfig
import ape.datahub.utils.DatahubUtils
import ape.datahub.utils.DatahubUtils.{createDataset, createLineage}
import ape.reader.Reader
import com.linkedin.common.urn.DatasetUrn
import com.sksamuel.avro4s.SchemaFor
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.{ClassTag, classTag}

class EmitterReader[E, ZE, T: SchemaFor :ClassTag] (
    r: Reader[E, ZE, T],
    upstreams:Seq[DatasetUrn] = Seq.empty,
    downstream:Option[DatasetUrn] = None,
    n: Option[String] = None
  ) extends Reader[E with DatahubConfig, ZE, T] {
  override def name: String = n.getOrElse(r.name)

  override protected[this] def read: ZIO[E with DatahubConfig, Throwable, ZStream[ZE, Throwable, T]] = for {
    urn <- createDataset[T]
    upstreamLineageResponse <- ZIO.when(upstreams.nonEmpty) {
      createLineage(upstreams, urn)
    }
    downstreamLineageResponse <- ZIO.when(downstream.isDefined) {
      createLineage(Seq(urn), downstream.orNull)
    }
    s <- r.apply
  } yield s

  def withUpstreams(up: Seq[DatasetUrn]) = new EmitterReader[E, ZE, T](r, up, downstream, n)
  def withDownstream(down: DatasetUrn) = new EmitterReader[E, ZE, T](r, upstreams, Some(down), n)
}


object EmitterReader {
  def fromReader[E, ZE, T: SchemaFor :ClassTag](reader: Reader[E, ZE, T]): EmitterReader[E, ZE, T] =
    new EmitterReader[E, ZE, T](reader)

  def apply[E, ZE, T: SchemaFor :ClassTag] (reader: Reader[E, ZE, T]): EmitterReader[E, ZE, T] =
    fromReader[E, ZE, T](reader)
}