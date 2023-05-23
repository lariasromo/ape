package ape.pipe

import ape.Ape.Transition
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class ZTPipe[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                                                               input: Pipe[E, ZE, T0, T1],
                                                               transform:ZStream[ZE, Throwable, T1] => ZStream[ZE, Throwable, T2],
                                                               n:String="ZTWriter"
                                                               ) extends Pipe[E, ZE, T0, T2]{
  override val transitions: Seq[Transition] = input.transitions ++
    Seq(
      Transition(
        implicitly[ClassTag[T1]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T2]].runtimeClass.getSimpleName
      )
    )

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = for {
    s <- input.apply(i)
  } yield transform(s)

  override def name: String = input.name
}