package com.libertexgroup.ape.writers
import zio.{Console, ZIO}
import zio.stream.ZStream

protected[writers] class ConsoleWriter[E, T] extends Writer[E, E, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E, Throwable, Unit] =
    stream.tap(r => zio.Console.printLine(r.toString)).runDrain
}
