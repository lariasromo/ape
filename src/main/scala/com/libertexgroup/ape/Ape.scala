package com.libertexgroup.ape

import com.libertexgroup.ape.Ape.Transition
import com.libertexgroup.ape.readers.PipelineReaders
import com.libertexgroup.ape.writers.PipelineWriters
import zio._
import zio.stream.{ZSink, ZStream}

import scala.reflect.ClassTag

case class Ape[ZE, T: ClassTag] (stream: ZStream[ZE, Throwable, T], transitions: Seq[Transition]){
  def run: ZIO[ZE, Throwable, Unit] = stream.runDrain
  def run[R1, E1, L, Z](sink: => ZSink[R1, E1, T, L, Z]): ZIO[R1 with ZE, Any, Z] = stream.run(sink)
  def -->[E2, T2: ClassTag](writer: Writer[E2, ZE, T, T2]): ZIO[E2, Throwable, Ape[ZE, T2]] =
    Ape.apply[E2, E2, ZE, T, T2](new Reader.UnitReader(stream), writer)
}

object Ape {
  case class Transition(t0: String, op:String, t1: String)
  def apply[E, E1, ZE, T0: ClassTag, T: ClassTag](
                               reader: Reader[E, ZE, T0],
                               writer: Writer[E1, ZE, T0, T]
                             ): ZIO[E with E1, Throwable, Ape[ZE, T]] = {
    for {
      s <- reader.apply
      s2 <- writer.apply(s)
    } yield Ape(s2, reader.transitions ++ writer.transitions)
  }

  val readers = new PipelineReaders()
  val writers = new PipelineWriters()
}