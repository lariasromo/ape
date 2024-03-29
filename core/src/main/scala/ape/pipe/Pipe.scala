package ape.pipe

import ape.metrics.ApeMetrics._
import ape.pipe.Pipe.concatenate
import ape.utils.Utils.{:=, reLayer}
import com.sksamuel.avro4s.SchemaFor
import zio.stream.ZStream
import zio.{Scope, Tag, ZIO}

import scala.reflect.{ClassTag, classTag}
import scala.util.Try

abstract class Pipe[-E, ZE, T0: ClassTag, T: ClassTag]{
  def name:String = this.getClass.getSimpleName

  protected[this] def pipe(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T]]

  def apply(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T]] =
    pipe(i).flatMap(s => ZIO.succeed(s.withMetrics(name)))

  def write(i: ZStream[ZE, Throwable, T0]): ZIO[ZE with E, Throwable, Unit] = for {
    s <- apply(i)
    _ <- s.runDrain
  } yield ()

  def runDrain(i: ZStream[ZE, Throwable, T0]): ZIO[ZE with E, Throwable, Unit] = write(i)

  def <*>[E2, T2: ClassTag](that: Pipe[E2, ZE, T0, T2]): Pipe[E with E2 with ZE with Scope, ZE, T0, Any] =
    Pipe.cross(this, that)

  def <*[E2, T2: ClassTag](that: Pipe[E2, ZE, T0, T2]): Pipe[E with E2 with ZE with Scope, ZE, T0, T] =
    Pipe.crossLeft(this, that)

  def *>[E2, T2: ClassTag](that: Pipe[E2, ZE, T0, T2]): Pipe[E with E2 with ZE with Scope, ZE, T0, T2] =
    Pipe.crossRight(this, that)

  def ++[E2, T2: ClassTag](that: Pipe[E2, ZE, T0, T2]): Pipe[E with E2 with ZE with Scope, ZE, T0, (T, T2)] =
    Pipe.zip(this, that)

  def -->[E2, T2: ClassTag](that: Pipe[E2, ZE, T, T2]): Pipe[E with E2, ZE, T0, T2] =
    Pipe.concatenate(this, that)

  def withTransform[T2: ClassTag](t: T => T2, name:String="withTransform"): Pipe[E, ZE, T0, T2] =
    new TTPipe(this, t, name)

  def map[T2: ClassTag](t: T => T2, name:String="map"): Pipe[E, ZE, T0, T2] =
    withTransform(t, name)

  def **[T2: ClassTag](implicit t: T => T2): Pipe[E, ZE, T0, T2] =
    withTransform(t)

  def contramapZZ[E2: Tag, T00: ClassTag] (
     t: T00 => ZIO[E2, Throwable, T0],
     name:String="contramapZZIO" ): Pipe[E with E2, ZE, T00, T] = {
    def tt(i: ZStream[ZE, Throwable, T00]): ZIO[E2, Throwable, ZStream[ZE, Throwable, T0]] =
      for {
        l <- reLayer[E2]
      } yield i.mapZIO(t(_).provideSomeLayer(l))
    new UnitPipe[E2, ZE, T00, T0](tt, name) --> this
  }

  def contramapZZZ[E2, T00: ClassTag] (
     t: ZStream[ZE, Throwable, T00] => ZIO[E2, Throwable, ZStream[ZE, Throwable, T0]],
     name:String="contramapZZIO"
   ): Pipe[E with E2, ZE, T00, T] = {
    concatenate(new UnitPipe(t, name), this)
  }

  def contramap[T00: ClassTag](t: T00 => T0, name:String="contramap"): Pipe[E, ZE, T00, T] =
    concatenate(new UnitTPipe(t, name), this)

  def withZTransform[T2: ClassTag](t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2], name:String="withZTransform"):
    Pipe[E, ZE, T0, T2] = new ZTPipe(this, t, name)

  def mapZ[T2: ClassTag](t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2], name:String="mapZ"): Pipe[E, ZE, T0, T2] =
    withZTransform(t, name)

  def ***[T2: ClassTag](implicit t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Pipe[E, ZE, T0, T2] =
    withZTransform(t)

  def tap(t: T => ZIO[ZE, Throwable, T], name:String="tap"): Pipe[E, ZE, T0, T] = mapZ(s => s.tap(t), name)

  def contramapZ[T00: ClassTag](t: ZStream[ZE, Throwable, T00] => ZStream[ZE, Throwable, T0], name:String="contramapZ"):
    Pipe[E, ZE, T00, T] = concatenate(new UnitZPipe(t, name), this)

  def safeGet[V :ClassTag]: Pipe[E, ZE, T0, V] = {
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
  def as[V :ClassTag]: Pipe[E, ZE, T0, V] = map(x => Try(x.asInstanceOf[V]).toOption).safeGet[V]

  def filter(predicate: T => Boolean, name:String="filter"): Pipe[E, ZE, T0, T] = mapZ(_.filter(predicate), name)

  def +++[E2]( pipes: Pipe[E2, ZE, T0, _]* ): Pipe[E with E2 with ZE with Scope, ZE, T0, T0] = {
    val ps: Seq[Pipe[E with E2, ZE, T0, _]] = Seq(this) ++ pipes
    Pipe.broadcast[E with E2, ZE, T0](ps : _*)
  }
}


object Pipe {
  def broadcast[E, ZE, T0: ClassTag]( pipes: Pipe[E, ZE, T0, _]* ):
  Pipe[E with ZE with Scope, ZE, T0, T0] =
    new Pipe[ZE with E with Scope, ZE, T0, T0] {
      override protected[this] def pipe(i: ZStream[ZE, Throwable, T0]):
        ZIO[ZE with E with Scope, Throwable, ZStream[ZE, Throwable, T0]]  =
        for {
          inputStreamCopies <- i.broadcast(pipes.length, 100)
          _ <- ZIO.foreach(
            inputStreamCopies.zip(pipes)
          ) {
            case (sourceStream, pipe) => for {
              s <- pipe(sourceStream)
              _ <- s.runDrain.fork
            } yield ()
          }
        } yield i
    }

  def broadcastOp[E, E2, ZE, T0 :ClassTag, T :ClassTag, T2 :ClassTag, T3 :ClassTag](
       writer1: Pipe[E, ZE, T0, T],
       writer2: Pipe[E2, ZE, T0, T2],
       op: (ZStream[ZE, Throwable, T], ZStream[ZE, Throwable, T2]) => ZStream[ZE, Throwable, T3],
       n:String="broadcastOp",
       maximumLag: Int=100,
   ): Pipe[E with E2 with ZE with Scope, ZE, T0, T3] =
    new Pipe[E with E2 with ZE with Scope, ZE, T0, T3] {
      override def name: String = n

      override protected[this] def pipe(i: ZStream[ZE, Throwable, T0]):
      ZIO[E with E2 with ZE with Scope, Throwable, ZStream[ZE, Throwable, T3]] =
        for {
          ss <- i.broadcast(2, maximumLag)
          s1 <- writer1(ss(0))
          s2 <- writer2(ss(1))
        } yield op(s1, s2)
    }

  //same input will be send to 2 writers, zipping the results and producing a tuple ot T and T2
  def zip[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                               writer1: Pipe[E, ZE, T0, T],
                                                               writer2: Pipe[E2, ZE, T0, T2],
                                                               maximumLag: Int=1
                                                             ): Pipe[E with E2 with ZE with Scope, ZE, T0, (T, T2)] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 zip s2,
      s"{(${writer1.name}) ++ (${writer2.name})}",
      maximumLag
    )

  //same input will be send to 2 writers and streams will be consumed sequentially this *> that
  def crossRight[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                      writer1: Pipe[E, ZE, T0, T],
                                                                      writer2: Pipe[E2, ZE, T0, T2],
                                                                      maximumLag: Int=1
                                                                    ): Pipe[E with E2 with ZE with Scope, ZE, T0, T2] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 *> s2,
      s"{(${writer1.name}) *> (${writer2.name})}",
      maximumLag
    )

  //same input will be send to 2 writers and streams will be consumed sequentially this <*> that
  def cross[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                 writer1: Pipe[E, ZE, T0, T],
                                                                 writer2: Pipe[E2, ZE, T0, T2],
                                                                 maximumLag: Int=1
                                                               ): Pipe[E with E2 with ZE with Scope, ZE, T0, Any] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 <*> s2,
      s"{(${writer1.name}) <*> (${writer2.name})}",
      maximumLag
    )



  //same input will be send to 2 writers and streams will be consumed sequentially this <* that
  def crossLeft[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                     writer1: Pipe[E, ZE, T0, T],
                                                                     writer2: Pipe[E2, ZE, T0, T2],
                                                                     maximumLag: Int=1
                                                                   ): Pipe[E with E2 with ZE with Scope, ZE, T0, T] =
    broadcastOp(writer1, writer2,
      (s1: ZStream[ZE, Throwable, T], s2: ZStream[ZE, Throwable, T2]) => s1 <* s2,
      s"{(${writer1.name}) <* (${writer2.name})}",
      maximumLag
    )

  // output of a writer will be passed to the second writer then the output T will be passed to the second writer,
  // producing an output of T2
  def concatenate[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                                       writer1: Pipe[E, ZE, T0, T],
                                                                       writer2: Pipe[E2, ZE, T, T2]
                                                                     ): Pipe[E with E2, ZE, T0, T2] =
    UnitPipe( i =>
      for {
        s <- writer1(i)
        s2 <- writer2(s)
      } yield s2
    )

  def TTPipe[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                                                                 w: Pipe[E, ZE, T0, T1],
                                                                 t:T1=>T2,
                                                                 n:String="TTPipe"
              )(implicit d: E := Any, d1: ZE := Any): Pipe[E, ZE, T0, T2] =
    new TTPipe[E, ZE, T0, T1, T2](w, t, n)

  def UnitPipe[E, ZE, T: ClassTag, T2: ClassTag] (
                 t: ZStream[ZE, Throwable, T] => ZIO[E, Throwable, ZStream[ZE, Throwable, T2]],
                 n:String = "UnitPipe"
               )(implicit d: E := Any, d1: ZE := Any): Pipe[E, ZE, T, T2] =
    new UnitPipe[E, ZE, T, T2](t, n)

  def UnitZPipe[E, ZE, T: ClassTag, T2: ClassTag] (
                  t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2],
                  n:String = "UnitZPipe"
                )(implicit d: E := Any, d1: ZE := Any): Pipe[E, ZE, T, T2] =
    new UnitZPipe[E, ZE, T, T2](t, n)

  def UnitTPipe[E, ZE, T: ClassTag, T2: ClassTag] (
                  t: T => T2,
                  n:String = "UnitTPipe"
                )(implicit d: E := Any, d1: ZE := Any): Pipe[E, ZE, T, T2] =
    new UnitTPipe[E, ZE, T, T2](t, n)

  def ZTPipe[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                  i: Pipe[E, ZE, T0, T1],
                  t:ZStream[ZE, Throwable, T1] => ZStream[ZE, Throwable, T2],
                  n:String="ZTPipe"
                 )(implicit d: E := Any, d1: ZE := Any): Pipe[E, ZE, T0, T2] =
    new ZTPipe[E, ZE, T0, T1, T2](i, t, n)
}


