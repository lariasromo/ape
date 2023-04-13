package com.libertexgroup.ape

import com.libertexgroup.ape.readers.PipelineReaders
import com.libertexgroup.ape.writers.PipelineWriters
import com.libertexgroup.metrics.ApeMetrics
import com.libertexgroup.metrics.ApeMetrics._
import zio._
import zio.stream.ZStream

case class Ape[ZE, T] (stream: ZStream[ZE, Throwable, T]){
  def run: ZIO[ZE, Throwable, Unit] = stream.runDrain
}

object Ape {
  def apply[E, E1, ZE, T0, T,
    R <: Reader[E, ZE, T0],
    W <: Writer[E1, ZE, T0, T]]( reader: R, writer: W ): ZIO[E with E1, Throwable, Ape[ZE, T]] = {
    for {
      s <- reader.apply
      s2 <- writer.apply(s.withMetrics(reader.getClass.getName))
    } yield Ape(s2.withMetrics(writer.getClass.getName))
  }

  val readers = new PipelineReaders()
  val writers = new PipelineWriters()
}