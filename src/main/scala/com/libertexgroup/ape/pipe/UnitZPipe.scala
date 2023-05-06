package com.libertexgroup.ape.pipe

import com.libertexgroup.ape.Ape.Transition
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class UnitZPipe[E, ZE, T: ClassTag, T2: ClassTag](
                                                      t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2],
                                                      n:String = "UnitZWriter"
                                                    ) extends Pipe[E, ZE, T, T2] {
  override val transitions: Seq[Transition] = Seq(
    Transition(
      implicitly[ClassTag[T]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T2]].runtimeClass.getSimpleName
    ))
  override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = ZIO.succeed(t(i))
  override val name: String = n
}