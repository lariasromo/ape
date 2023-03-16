package com.libertexgroup.ape.pipelines

import com.libertexgroup.ape.readers.{PipelineReaders, Reader}
import com.libertexgroup.ape.transformers.Transformer
import com.libertexgroup.ape.writers.{PipelineWriters, Writer}
import zio.{Console, Tag, ZIO}

import scala.reflect.ClassTag

class Pipeline[E, E1, T: ClassTag, T1: ClassTag, E2](
                                  reader: Reader[E, E1, T],
                                  transformer: Transformer[E1, T, T1],
                                  writer: Writer[E1, E2, T1]
                                ) {

  def run: ZIO[E with E2, Throwable, Unit] = for {
    stream <- reader.apply
    transformedStream = transformer.apply(stream)
    _ <- writer.apply(transformedStream)
      .catchAll(e => Console.printLine(e.toString))
  } yield ()
}

object Pipeline{
  val readers = new PipelineReaders()
  val writers = new PipelineWriters()
}