package ape.pipe

import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class UnitPipe[E, ZE, T: ClassTag, T2: ClassTag](
                                                     t: ZStream[ZE, Throwable, T] => ZIO[E, Throwable, ZStream[ZE, Throwable, T2]],
                                                     n:String = "UnitWriter"
                                                   )
  extends Pipe[E, ZE, T, T2] {

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = t(i)

  override val name: String = n
}