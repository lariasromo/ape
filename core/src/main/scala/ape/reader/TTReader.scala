package ape.reader

import ape.Ape.Transition
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class TTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                                                   input: Reader[E, ZE, T0],
                                                   transform:T0=>T1, n:String="TTReader"
                                                 ) extends Reader[E, ZE, T1] {
  override def read: ZIO[E, Throwable, ZStream[ZE, Throwable, T1]] = for {
    s <- input.apply
  } yield s.map(transform)

  override def name: String = input.name
}