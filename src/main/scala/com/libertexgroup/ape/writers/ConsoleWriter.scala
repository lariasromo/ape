package com.libertexgroup.ape.writers
import com.libertexgroup.ape.Writer
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[writers] class ConsoleWriter[E, ET, T: ClassTag] extends Writer[E, ET, T, T] {
  override def apply(i: ZStream[ET, Throwable, T]): ZIO[E, Throwable, ZStream[ET, Throwable, T]] =
    ZIO.succeed(i.tap(r => zio.Console.printLine(r.toString)))
}
