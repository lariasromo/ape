package com.libertexgroup.ape

import com.libertexgroup.ape.readers.PipelineReaders
import com.libertexgroup.ape.writers.PipelineWriters
import zio._
import zio.stream.ZStream

import scala.reflect.ClassTag

case class Ape[ZE, T: ClassTag] (stream: ZStream[ZE, Throwable, T]){
  def run: ZIO[ZE, Throwable, Unit] = stream.runDrain
  def -->[E2, T2: ClassTag](writer: Writer[E2, ZE, T, T2]): ZIO[E2, Throwable, Ape[ZE, T2]] =
    Ape.apply[E2, E2, ZE, T, T2](new Reader.UnitReader(stream), writer)
}

object Ape {
  def apply[E, E1, ZE, T0: ClassTag, T: ClassTag](
                               reader: Reader[E, ZE, T0],
                               writer: Writer[E1, ZE, T0, T]
                             ): ZIO[E with E1, Throwable, Ape[ZE, T]] = {
    for {
      s <- reader.apply
      s2 <- writer.apply(s)
    } yield Ape(s2)
  }

  val readers = new PipelineReaders()
  val writers = new PipelineWriters()
}