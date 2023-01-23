package com.libertexgroup.algebras.writers
import zio.ZIO
import zio.console.{Console, putStrLn}
import zio.stream.ZStream

class DefaultWriter[E, T] extends Writer[E, Console with E, T] {
  override def apply(stream: ZStream[E, Throwable, T]): ZIO[Console with E, Throwable, Unit] =
    stream.tap(r => putStrLn(r.toString)).runDrain
}
