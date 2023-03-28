package com.libertexgroup.ape.writers
import com.libertexgroup.ape.Writer
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[writers] class ConsoleWriter[E, T: ClassTag] extends Writer[E, E, T, T] {
  override def apply(i: ZStream[E, Throwable, T]): ZIO[E, Throwable, ZStream[E, Throwable, T]] =
    ZIO.succeed(i.tap(r => zio.Console.printLine(r.toString)))
}
