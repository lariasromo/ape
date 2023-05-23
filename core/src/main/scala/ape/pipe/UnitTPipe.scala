package ape.pipe

import ape.Ape.Transition
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class UnitTPipe[E, ZE, T: ClassTag, T2: ClassTag](
                                                      t: T => T2,
                                                      n:String = "UnitTWriter"
                                                    ) extends Pipe[E, ZE, T, T2]{
  override val transitions: Seq[Transition] = Seq(
    Transition(
      implicitly[ClassTag[T]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T2]].runtimeClass.getSimpleName
    ))
  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] =
    ZIO.succeed(i.map(t))
  override val name: String = n
}