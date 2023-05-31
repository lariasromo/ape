package ape.reader

import ape.Ape.Transition
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class UnitReader[E, ZE, T: ClassTag] (
                                       stream: ZStream[ZE, Throwable, T],
                                       n:String = "UnitReader"
                                     ) extends Reader[E, ZE, T] {
  override def read: ZIO[E, Throwable, ZStream[ZE, Throwable, T]] = ZIO.succeed(stream)

  override def name: String = n
}