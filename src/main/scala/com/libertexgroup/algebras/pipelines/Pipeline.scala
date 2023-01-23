package com.libertexgroup.algebras.pipelines

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.algebras.transformers.Transformer
import com.libertexgroup.algebras.writers.Writer
import zio.console.{Console, putStrLn}
import zio.{Has, Tag, ZIO}

object Pipeline {
  def apply[E0 : Tag, E : Tag, E1: Tag, T: Tag, T1: Tag]: ZIO[Console with E1 with E0
    with Has[Reader[E0, E, T]]
    with Has[Transformer[E, T, T1]]
    with Has[Writer[E, E1, T1]], Throwable, Unit] =
    for {
      reader <- ZIO.access[Has[Reader[E0, E, T]]](_.get)
      transformer <- ZIO.access[Has[Transformer[E, T, T1]]](_.get)
      writer <- ZIO.access[Has[Writer[E, E1, T1]]](_.get)

      stream <- reader.apply
      transformedStream = transformer.apply(stream)
      _ <- writer.apply(transformedStream)
        .catchAll(e => putStrLn(e.toString))
    } yield ()

  def createWithETL[E0 : Tag, E : Tag, T : Tag, T1 : Tag, E1 : Tag]
  (reader: Reader[E0, E, T], transformer: Transformer[E, T, T1], writer: Writer[E, E1, T1]):
    ZIO[Console with E1 with E0, Throwable, Unit] =
    for {
    stream <- reader.apply
    transformedStream = transformer.apply(stream)
    _ <- writer.apply(transformedStream)
      .catchAll(e => putStrLn(e.toString))
  } yield ()
}
