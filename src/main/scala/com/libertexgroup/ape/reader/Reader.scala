package com.libertexgroup.ape.reader

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.Ape.Transition
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.metrics.ApeMetrics._
import com.libertexgroup.utils.Utils.:=
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.{ClassTag, classTag}
import scala.util.Try

abstract class Reader[E, ZE, T: ClassTag]{
  def name:String = this.getClass.getSimpleName
  def transitions: Seq[Transition] = Seq(Transition("|", name, implicitly[ClassTag[T]].runtimeClass.getSimpleName))

  protected[this] def read: ZIO[E, Throwable, ZStream[ZE, Throwable, T]]

  def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, T]] = for {
    s <- read
  } yield s.withMetrics(name)

  def pipe: Pipe[E, ZE, T, T] = Pipe.UnitWriter[E, ZE, T, T](_ => apply)

  def stream: ZStream[ZE with E, Throwable, T] = ZStream.unwrap(apply)

  def ape[E2, T2: ClassTag](writer: Pipe[E2, ZE, T, T2]): ZIO[E with E2, Throwable, Ape[ZE, T2]] =
    Ape.apply[E, E2, ZE, T, T2](this, writer)

  def -->[E2, T2: ClassTag](that: Pipe[E2, ZE, T, T2]): ZStream[ZE with E with E2, Throwable, T2] =
    Ape.readWrite(this, that)

  def ->>[E2, T2: ClassTag](writer: Pipe[E2, ZE, T, T2]): ZIO[ZE with E with E2, Throwable, Unit] =
    for {
      s <- ape(writer)
      _ <- s.run
    } yield ()

  def withTransform[T2: ClassTag](t: T => T2, name:String="withTransform"): Reader[E, ZE, T2] =
    new TTReader(this, t, name)
  def **[T2: ClassTag](implicit t: T => T2): Reader[E, ZE, T2] =
    withTransform(t, "**")
  def map[T2: ClassTag](t: T => T2, name:String="map"): Reader[E, ZE, T2] =
    withTransform(t, name)

  def withZTransform[T2: ClassTag](
                                    t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2],
                                    name:String="withZTransform"
                                  ): Reader[E, ZE, T2] = new ZTReader(this, t, name)
  def ***[T2: ClassTag](implicit t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Reader[E, ZE, T2] =
    withZTransform(t, "***")
  def mapZ[T2: ClassTag](
                          t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2],
                          name:String="mapZ"
                        ): Reader[E, ZE, T2] = withZTransform(t, name)

  def safeGet[V :ClassTag]: Reader[E, ZE, V] = {
    implicit class ClassTagOps[U](val classTag: ClassTag[U]){
      def <<:(other: ClassTag[_]): Boolean = classTag.runtimeClass.isAssignableFrom(other.runtimeClass)
    }
    classTag[T] match {
      case a if a <<: classTag[Option[V]] =>
        this.map(_.asInstanceOf[Option[V]]).mapZ(stream => stream.filter(_.isDefined).map(_.get))
      case a if a <<: classTag[V] => this.map(_.asInstanceOf[V])
      case _ => throw new Exception(s"Cannot safely get values, " +
        s"${classTag[V].runtimeClass.toString} should be Option[${classTag[T].runtimeClass.toString}]")
    }
  }

  def as[V :ClassTag]: Reader[E, ZE, V] = map(x => Try(x.asInstanceOf[V]).toOption).safeGet[V]

  def filter(predicate: T => Boolean, name:String="filter"): Reader[E, ZE, T] = mapZ(_.filter(predicate), name)
}

object Reader {

  def TTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                  input: Reader[E, ZE, T0],
                  transform:T0=>T1, n:String="TTReader"
                )(implicit d: E := Any, d1: ZE := Any): Reader[E, ZE, T1] =
    new TTReader[E, ZE, T0, T1](input, transform, n)

  def ZTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                 input: Reader[E, ZE, T0],
                 transform:ZStream[ZE, Throwable, T0] => ZStream[ZE, Throwable, T1],
                 n:String="ZTReader"
               )(implicit d: E := Any, d1: ZE := Any): Reader[E, ZE, T1] =
    new ZTReader[E, ZE, T0, T1](input, transform, n)

  def UnitReader[E, ZE, T: ClassTag](
                  stream: ZStream[ZE, Throwable, T],
                  n:String = "UnitReader"
                )(implicit d: E := Any, d1: ZE := Any): Reader[E, ZE, T] =
    new UnitReader[E, ZE, T](stream, n)
}

