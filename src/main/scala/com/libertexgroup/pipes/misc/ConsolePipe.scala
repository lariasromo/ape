package com.libertexgroup.pipes.misc

import com.libertexgroup.ape.pipe.Pipe
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[misc] class ConsolePipe[E, ET, T: ClassTag] extends Pipe[E, ET, T, T] {
  override val name: String = "ConsoleWriter"

  override protected[this] def pipe(i: ZStream[ET, Throwable, T]): ZIO[E, Throwable, ZStream[ET, Throwable, T]] =
    ZIO.succeed(i.tap(r => zio.Console.printLine(r.toString)))
}
