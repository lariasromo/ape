package com.libertexgroup.ape

import com.libertexgroup.ape.readers.PipelineReaders
import com.libertexgroup.ape.writers.PipelineWriters
import com.libertexgroup.metrics.ApeMetrics._
import zio._
import zio.stream.ZStream

case class Ape[ZE, T] (stream: ZStream[ZE, Throwable, T]){
  def run: ZIO[ZE, Throwable, Unit] = stream.runDrain
}

object Ape {
  def apply[E, E1, ZE, T0, T](
                               reader: Reader[E, ZE, T0],
                               writer: Writer[E1, ZE, T0, T]
                             ): ZIO[E with E1, Throwable, Ape[ZE, T]] = {
    for {
      s <- reader.apply
      s2 <- writer.apply(s.withMetrics(reader.name).withMetrics(writer.name))
    } yield Ape(s2)
  }

  val readers = new PipelineReaders()
  val writers = new PipelineWriters()
}