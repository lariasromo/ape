package ape.datahub.pipe

import ape.datahub.configs.DatahubConfig
import ape.pipe.Pipe
import com.linkedin.common.urn.DatasetUrn
import com.sksamuel.avro4s.SchemaFor

import scala.reflect.ClassTag

object EmitterPipe {
  def fromPipeBoth[E, ZE,  T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag] (
    pipe: Pipe[E, ZE, T1, T2],
    upstreams: Seq[DatasetUrn] = Seq.empty,
    downstream: Option[DatasetUrn] = None,
    name: Option[String] = None,
  ): Pipe[E with DatahubConfig, ZE, T1, T2] =
    new EmitterPipeBoth[E, ZE, T1, T2](pipe, EmitterType.BOTH, upstreams = upstreams, downstream = downstream, n = name)

  def fromPipeRight[E, ZE, T1:ClassTag, T2 :SchemaFor :ClassTag] (
    pipe: Pipe[E, ZE, T1, T2],
    upstreams: Seq[DatasetUrn] = Seq.empty,
    downstream: Option[DatasetUrn] = None,
    name: Option[String] = None
  ): Pipe[E with DatahubConfig, ZE, T1, T2] =
    new EmitterPipeRight[E, ZE, T1, T2](pipe, EmitterType.RIGHT, upstreams = upstreams, downstream = downstream, n = name)

  def fromPipeLeft[E, ZE, T1 :SchemaFor :ClassTag, T2 :ClassTag] (
    pipe: Pipe[E, ZE, T1, T2],
    upstreams: Seq[DatasetUrn] = Seq.empty,
    downstream: Option[DatasetUrn] = None,
    name: Option[String] = None
  ): Pipe[E with DatahubConfig, ZE, T1, T2] =
    new EmitterPipeLeft[E, ZE, T1, T2](pipe, EmitterType.RIGHT, upstreams = upstreams, downstream = downstream, n = name)

  def apply[E, ZE, T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag] (
    pipe: Pipe[E, ZE, T1, T2],
    upstreams: Seq[DatasetUrn] = Seq.empty,
    downstream: Option[DatasetUrn] = None,
    name: Option[String] = None,
  ): Pipe[E with DatahubConfig, ZE, T1, T2] =  fromPipeBoth(pipe, upstreams, downstream, name)
}
