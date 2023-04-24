package com.libertexgroup.ape

import com.libertexgroup.ape.Ape.Transition
import com.libertexgroup.ape.Writer._
import com.libertexgroup.metrics.ApeMetrics._
import zio.stream.ZStream
import zio.{Scope, ZIO}

import scala.reflect.{ClassTag, classTag}
import scala.util.Try

abstract class Writer[-E, ZE, T0: ClassTag, T: ClassTag]{
  def name:String = this.getClass.getSimpleName
  def transitions: Seq[Transition] = Seq(
    Transition(
      implicitly[ClassTag[T0]].runtimeClass.getSimpleName, name, implicitly[ClassTag[T]].runtimeClass.getSimpleName
    ))
  protected[this] def pipe(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T]]
  def apply(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T]] =
    pipe(i).flatMap(s => ZIO.succeed(s.withMetrics(name)))
  def write(i: ZStream[ZE, Throwable, T0]): ZIO[ZE with E, Throwable, Unit] = for {
    s <- apply(i)
    _ <- s.runDrain
  } yield ()
  def runDrain(i: ZStream[ZE, Throwable, T0]): ZIO[ZE with E, Throwable, Unit] = write(i)

  def <*>[E2, T2: ClassTag](that: Writer[E2, ZE, T0, T2]): Writer[E with E2 with ZE with Scope, ZE, T0, Any] =
    Writer.cross(this, that)

  def <*[E2, T2: ClassTag](that: Writer[E2, ZE, T0, T2]): Writer[E with E2 with ZE with Scope, ZE, T0, T] =
    Writer.crossLeft(this, that)

  def *>[E2, T2: ClassTag](that: Writer[E2, ZE, T0, T2]): Writer[E with E2 with ZE with Scope, ZE, T0, T2] =
    Writer.crossRight(this, that)

  def >>>[E2, T2: ClassTag](that: Writer[E2, ZE, T0, T2]): Writer[E with E2 with ZE with Scope, ZE, T0, Any] =
    Writer.interleave(this, that)

  def <+>[E2, T2: ClassTag](that: Writer[E2, ZE, T0, T2]): Writer[E with E2 with ZE with Scope, ZE, T0, Any] =
    Writer.merge(this, that)

  def <+[E2, T2: ClassTag](that: Writer[E2, ZE, T0, T2]): Writer[E with E2 with ZE with Scope, ZE, T0, T] =
    Writer.mergeLeft(this, that)

  def +>[E2, T2: ClassTag](that: Writer[E2, ZE, T0, T2]): Writer[E with E2 with ZE with Scope, ZE, T0, T2] =
    Writer.mergeRight(this, that)

  def ++[E2, T2: ClassTag](that: Writer[E2, ZE, T0, T2]): Writer[E with E2 with ZE with Scope, ZE, T0, (T, T2)] =
    Writer.zip(this, that)

  def -->[E2, T2: ClassTag](that: Writer[E2, ZE, T, T2]): Writer[E with E2, ZE, T0, T2] =
    Writer.concatenate(this, that)

  def withTransform[T2: ClassTag](t: T => T2, name:String="withTransform"): Writer[E, ZE, T0, T2] =
    new TTWriter(this, t, name)
  def map[T2: ClassTag](t: T => T2, name:String="map"): Writer[E, ZE, T0, T2] =
    withTransform(t, name)
  def **[T2: ClassTag](implicit t: T => T2): Writer[E, ZE, T0, T2] =
    withTransform(t)

  def contramap[T00: ClassTag](t: T00 => T0, name:String="contramap"): Writer[E, ZE, T00, T] =
    concatenate(new UnitTWriter(t, name), this)

  def withZTransform[T2: ClassTag](t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2], name:String="withZTransform"):
    Writer[E, ZE, T0, T2] = new ZTWriter(this, t, name)
  def mapZ[T2: ClassTag](t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2], name:String="mapZ"): Writer[E, ZE, T0, T2] =
    withZTransform(t, name)
  def ***[T2: ClassTag](implicit t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Writer[E, ZE, T0, T2] =
    withZTransform(t)

  def contramapZ[T00: ClassTag](t: ZStream[ZE, Throwable, T00] => ZStream[ZE, Throwable, T0], name:String="contramapZ"):
    Writer[E, ZE, T00, T] = concatenate(new UnitZWriter(t, name), this)

  def safeGet[V :ClassTag]: Writer[E, ZE, T0, V] = {
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
  def as[V :ClassTag]: Writer[E, ZE, T0, V] = map(x => Try(x.asInstanceOf[V]).toOption).safeGet[V]

  def filter(predicate: T => Boolean, name:String="filter"): Writer[E, ZE, T0, T] = mapZ(_.filter(predicate), name)
}


object Writer {
  def broadcastOp[E, E2, ZE, T0 :ClassTag, T :ClassTag, T2 :ClassTag, T3 :ClassTag](
                                             writer1: Writer[E, ZE, T0, T],
                                             writer2: Writer[E2, ZE, T0, T2],
                                             op: (ZStream[ZE, Throwable, T], ZStream[ZE, Throwable, T2]) => ZStream[ZE, Throwable, T3],
                                             n:String="broadcastOp",
                                             maximumLag: Int=100,
                                           ): Writer[E with E2 with ZE with Scope, ZE, T0, T3] =
    new Writer[E with E2 with ZE with Scope, ZE, T0, T3] {
      override def name: String = n

      override val transitions: Seq[Transition] = writer1.transitions ++
        Seq(
          Transition(
            implicitly[ClassTag[T]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T3]].runtimeClass.getSimpleName
          )
        ) ++ writer2.transitions

      override protected[this] def pipe(i: ZStream[ZE, Throwable, T0]):
      ZIO[E with E2 with ZE with Scope, Throwable, ZStream[ZE, Throwable, T3]] =
        for {
          ss <- i.broadcast(2, maximumLag)
          s1 <- writer1(ss(0))
          s2 <- writer2(ss(1))
        } yield op(s1, s2)
    }

  //same input will be send to 2 writers, merging the results (as they come) and producing Any
  def merge[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                 writer1: Writer[E, ZE, T0, T],
                                                                 writer2: Writer[E2, ZE, T0, T2],
                                                                 maximumLag: Int=1
                                                               ): Writer[E with E2 with ZE with Scope, ZE, T0, Any] = {

    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 merge s2,
      s"{(${writer1.name}) <+> (${writer2.name})}",
      maximumLag
    )
  }

  //same input will be send to 2 writers, merging the results (as they come) and discarding results from the 2nd writer
  def mergeLeft[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                     writer1: Writer[E, ZE, T0, T],
                                                                     writer2: Writer[E2, ZE, T0, T2],
                                                                     maximumLag: Int=1
                                                                   ): Writer[E with E2 with ZE with Scope, ZE, T0, T] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 mergeLeft s2,
      s"{${writer1.name}) <+ (${writer2.name})}",
      maximumLag
    )

  //same input will be send to 2 writers, merging the results (as they come) and discarding results from the 1st writer
  def mergeRight[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                      writer1: Writer[E, ZE, T0, T],
                                                                      writer2: Writer[E2, ZE, T0, T2],
                                                                      maximumLag: Int=1
                                                                    ): Writer[E with E2 with ZE with Scope, ZE, T0, T2] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 mergeRight s2,
      s"{(${writer1.name}) +> (${writer2.name})}",
      maximumLag
    )

  //same input will be send to 2 writers, interleaving the results (1 by 1 elements in each stream) and producing Any
  def interleave[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                       writer1: Writer[E, ZE, T0, T],
                                                                       writer2: Writer[E2, ZE, T0, T2],
                                                                       maximumLag: Int=1
                                                                     ): Writer[E with E2 with ZE with Scope, ZE, T0, Any] = {
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 interleave s2,
      s"{(${writer1.name}) interleave (${writer2.name})}",
      maximumLag
    )
  }

  //same input will be send to 2 writers, zipping the results and producing a tuple ot T and T2
  def zip[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                               writer1: Writer[E, ZE, T0, T],
                                                               writer2: Writer[E2, ZE, T0, T2],
                                                               maximumLag: Int=1
                                                             ): Writer[E with E2 with ZE with Scope, ZE, T0, (T, T2)] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 zip s2,
      s"{(${writer1.name}) ++ (${writer2.name})}",
      maximumLag
    )

  //same input will be send to 2 writers and streams will be consumed sequentially this *> that
  def crossRight[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                      writer1: Writer[E, ZE, T0, T],
                                                                      writer2: Writer[E2, ZE, T0, T2],
                                                                      maximumLag: Int=1
                                                                    ): Writer[E with E2 with ZE with Scope, ZE, T0, T2] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 *> s2,
      s"{(${writer1.name}) *> (${writer2.name})}",
      maximumLag
    )

  //same input will be send to 2 writers and streams will be consumed sequentially this <*> that
  def cross[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                 writer1: Writer[E, ZE, T0, T],
                                                                 writer2: Writer[E2, ZE, T0, T2],
                                                                 maximumLag: Int=1
                                                               ): Writer[E with E2 with ZE with Scope, ZE, T0, Any] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 <*> s2,
      s"{(${writer1.name}) <*> (${writer2.name})}",
      maximumLag
    )



  //same input will be send to 2 writers and streams will be consumed sequentially this <* that
  def crossLeft[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                     writer1: Writer[E, ZE, T0, T],
                                                                     writer2: Writer[E2, ZE, T0, T2],
                                                                     maximumLag: Int=1
                                                                   ): Writer[E with E2 with ZE with Scope, ZE, T0, T] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 <* s2,
      s"{(${writer1.name}) <* (${writer2.name})}",
      maximumLag
    )

  // output of a writer will be passed to the second writer then the output T will be passed to the second writer,
  // producing an output of T2
  def concatenate[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                       writer1: Writer[E, ZE, T0, T],
                                                                       writer2: Writer[E2, ZE, T, T2]
                                                                     ): Writer[E with E2, ZE, T0, T2] =
    new Writer.UnitWriter( i =>
      for {
        s <- writer1(i)
        s2 <- writer2(s)
      } yield s2
    )


  class TTWriter[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                                                                   writer: Writer[E, ZE, T0, T1],
                                                                   transform:T1=>T2,
                                                                   n:String="TTWriter"
                                                                 ) extends Writer[E, ZE, T0, T2] {
    override val transitions: Seq[Transition] = writer.transitions ++
      Seq(
        Transition(
          implicitly[ClassTag[T1]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T2]].runtimeClass.getSimpleName
        )
      )

    override protected[this] def pipe(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = for {
      s <- writer.apply(i)
    } yield s.map(transform)

    override def name: String = writer.name
  }

  class ZTWriter[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                                                                   input: Writer[E, ZE, T0, T1],
                                                                   transform:ZStream[ZE, Throwable, T1] => ZStream[ZE, Throwable, T2],
                                                                   n:String="ZTWriter"
                                                                 ) extends Writer[E, ZE, T0, T2]{
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

  class UnitWriter[E, ZE, T: ClassTag, T2: ClassTag] (
                                                       t: ZStream[ZE, Throwable, T] => ZIO[E, Throwable, ZStream[ZE, Throwable, T2]],
                                                       n:String = "UnitWriter"
                                                     )
    extends Writer[E, ZE, T, T2] {
    override val transitions: Seq[Transition] = Seq(
      Transition(
        implicitly[ClassTag[T]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T2]].runtimeClass.getSimpleName
      ))

    override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = t(i)

    override val name: String = n
  }

  class UnitZWriter[E, ZE, T: ClassTag, T2: ClassTag] (
                                                        t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2],
                                                        n:String = "UnitZWriter"
                                                      ) extends Writer[E, ZE, T, T2]{
    override val transitions: Seq[Transition] = Seq(
      Transition(
        implicitly[ClassTag[T]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T2]].runtimeClass.getSimpleName
      ))
    override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = ZIO.succeed(t(i))
    override val name: String = n
  }


  class UnitTWriter[E, ZE, T: ClassTag, T2: ClassTag] (
                                                        t: T => T2,
                                                        n:String = "UnitTWriter"
                                                      ) extends Writer[E, ZE, T, T2]{
    override val transitions: Seq[Transition] = Seq(
      Transition(
        implicitly[ClassTag[T]].runtimeClass.getSimpleName, n, implicitly[ClassTag[T2]].runtimeClass.getSimpleName
      ))
    override protected[this] def pipe(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] =
      ZIO.succeed(i.map(t))
    override val name: String = n
  }
}


