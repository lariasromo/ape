package com.libertexgroup.ape.writers
import zio.{Console, Scope, ZIO}
import zio.stream.{ZSink, ZStream}

protected[writers] class ConsoleWriter[E, T] extends Writer[E, E with Scope, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[E with Scope, Throwable, Unit] =
    stream.tap(r => zio.Console.printLine(r.toString)).runScoped(ZSink.drain)
}
