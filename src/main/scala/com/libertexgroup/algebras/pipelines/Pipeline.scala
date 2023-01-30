package com.libertexgroup.algebras.pipelines

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.algebras.transformers.Transformer
import com.libertexgroup.algebras.writers.Writer
import zio.{Console, Tag, ZIO}

object Pipeline {
  def apply[E0 : Tag, E : Tag, T : Tag, T1 : Tag, E1 : Tag]
  (reader: Reader[E0, E, T], transformer: Transformer[E, T, T1], writer: Writer[E, E1, T1]):
    ZIO[Console with E1 with E0, Throwable, Unit] =
    for {
    stream <- reader.apply
    transformedStream = transformer.apply(stream)
    _ <- writer.apply(transformedStream)
      .catchAll(e => Console.printLine(e.toString))
  } yield ()
}
