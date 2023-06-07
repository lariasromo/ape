package ape.misc.pipes

import ape.pipe.Pipe
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

protected[misc] class ConsolePipe[E, ET, T: ClassTag] extends Pipe[E, ET, T, T] {
  override protected[this] def pipe(i: ZStream[ET, Throwable, T]): ZIO[E, Throwable, ZStream[ET, Throwable, T]] =
    ZIO.succeed(i.tap(r => ZIO.logInfo(r.toString)))
}
