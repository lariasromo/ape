package ape.reader

import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class ZTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                                                   input: Reader[E, ZE, T0],
                                                   transform:ZStream[ZE, Throwable, T0] => ZStream[ZE, Throwable, T1],
                                                   n:String="ZTReader"
                                                 ) extends Reader[E, ZE, T1]{
  override def read: ZIO[E, Throwable, ZStream[ZE, Throwable, T1]] = for {
    s <- input.apply
  } yield transform(s)

  override def name: String = input.name
}