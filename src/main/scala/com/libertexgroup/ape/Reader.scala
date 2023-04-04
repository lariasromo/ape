package com.libertexgroup.ape

import com.libertexgroup.ape.Reader.{TTReader, ZTReader}
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

abstract class Reader[E, ZE, T: ClassTag]{
  def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, T]]
  def read: ZIO[E, Throwable, ZStream[ZE, Throwable, T]] = apply
  def -->[E2, T2: ClassTag](writer: Writer[E2, ZE, T, T2]): ZIO[E with E2, Throwable, Ape[ZE, T2]] = Ape.apply(this, writer)
  
  def **[T2: ClassTag](implicit t: T => T2): Reader[E, ZE, T2] = new TTReader(this, t)
  def withTransform[T2: ClassTag](t: T => T2): Reader[E, ZE, T2] = {
    implicit val tt = t
    **
  }
  def map[T2: ClassTag](t: T => T2): Reader[E, ZE, T2] = withTransform(t)

  def ***[T2: ClassTag](implicit t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Reader[E, ZE, T2] = 
    new ZTReader(this, t)
  def withZTransform[T2: ClassTag](t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Reader[E, ZE, T2] = {
    implicit val tt = t
    ***
  }
  def mapZ[T2: ClassTag](t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Reader[E, ZE, T2] = withZTransform(t)
}

object Reader {
  class TTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                                                     input: Reader[E, ZE, T0],
                                                     transform:T0=>T1
                                                   ) extends Reader[E, ZE, T1] {
    override def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, T1]] = for {
      s <- input.apply
    } yield s.map(transform)
  }

  class ZTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                                                     input: Reader[E, ZE, T0],
                                                     transform:ZStream[ZE, Throwable, T0] => ZStream[ZE, Throwable, T1]
                                                   ) extends Reader[E, ZE, T1]{
    override def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, T1]] = for {
      s <- input.apply
    } yield transform(s)
  }

  class UnitReader[E, ZE, T: ClassTag] (stream: ZStream[ZE, Throwable, T]) extends Reader[E, ZE, T]{
    override def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, T]] = ZIO.succeed(stream)
  }
}

