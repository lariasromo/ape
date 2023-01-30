package com.libertexgroup.algebras.writers
import zio.{Console, ZIO}
import zio.stream.ZStream

class DefaultWriter[E, T] extends Writer[E, Console with E, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[Console with E, Throwable, Unit] =
    stream.tap(r => zio.Console.printLine(r.toString)).runDrain
}
