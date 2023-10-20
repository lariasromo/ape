package ape.pipe

import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class UnitPipeTap[E, ZE, T: ClassTag](
                                                     t: T => ZIO[ZE, Throwable, Any],
                                                     n:String = "UnitWriter"
                                                   )
  extends Pipe[E, ZE, T, T] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T]] =
    ZIO.attempt(i.tap(t))

  override val name: String = n
}