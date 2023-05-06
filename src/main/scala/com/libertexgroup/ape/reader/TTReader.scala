package com.libertexgroup.ape.reader

import com.libertexgroup.ape.Ape.Transition
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class TTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                                                   input: Reader[E, ZE, T0],
                                                   transform:T0=>T1, n:String="TTReader"
                                                 ) extends Reader[E, ZE, T1] {
  override val transitions: Seq[Transition] = input.transitions ++
    Seq(
      Transition(
        implicitly[ClassTag[T0]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T1]].runtimeClass.getSimpleName
      )
    )
  override def read: ZIO[E, Throwable, ZStream[ZE, Throwable, T1]] = for {
    s <- input.apply
  } yield s.map(transform)

  override def name: String = input.name
}