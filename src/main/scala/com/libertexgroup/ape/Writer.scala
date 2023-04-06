package com.libertexgroup.ape

import com.libertexgroup.ape.Writer.{TTWriter, UnitTWriter, UnitZWriter, ZTWriter, concatenate}
import com.libertexgroup.ape.readers.PipelineReaders
import com.libertexgroup.ape.writers.PipelineWriters
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

abstract class Writer[-E, ZE, T0: ClassTag, T: ClassTag]{
  def apply(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T]]
  def write(i: ZStream[ZE, Throwable, T0]): ZIO[ZE with E, Throwable, Unit] = for {
    s <- apply(i)
    _ <- s.runDrain
  } yield ()
  def runDrain(i: ZStream[ZE, Throwable, T0]): ZIO[ZE with E, Throwable, Unit] = write(i)

  def ++[E2, T2: ClassTag](writer2: Writer[E2, ZE, T0, T2]): Writer[E with E2, ZE, T0, (T, T2)] =
    Writer.sum(this, writer2)
  def -->[E2, T2: ClassTag](writer2: Writer[E2, ZE, T, T2]): Writer[E with E2, ZE, T0, T2] =
    Writer.concatenate(this, writer2)
  def <--[E2, T00: ClassTag](writer2: Writer[E2, ZE, T00, T0]): Writer[E2 with E, ZE, T00, T] =
    Writer.concatenate(writer2, this)


  def withTransform[T2: ClassTag](t: T => T2): Writer[E, ZE, T0, T2] = new TTWriter(this, t)
  def map[T2: ClassTag](t: T => T2): Writer[E, ZE, T0, T2] = withTransform(t)
  def **[T2: ClassTag](implicit t: T => T2): Writer[E, ZE, T0, T2] = withTransform(t)

  def contramap[T00: ClassTag](t: T00 => T0): Writer[E, ZE, T00, T] = concatenate(new UnitTWriter(t), this)

  def withZTransform[T2: ClassTag](t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Writer[E, ZE, T0, T2] =
    new ZTWriter(this, t)
  def mapZ[T2: ClassTag](t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Writer[E, ZE, T0, T2] =
    withZTransform(t)
  def ***[T2: ClassTag](implicit t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Writer[E, ZE, T0, T2] =
    withZTransform(t)

  def contramapZ[T00: ClassTag](t: ZStream[ZE, Throwable, T00] => ZStream[ZE, Throwable, T0]): Writer[E, ZE, T00, T] =
    concatenate(new UnitZWriter(t), this)
}

object Writer {
  //same input stream will be pass to 2 writers, producing a tuple ot T and T2
  def sum[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                                               writer1: Writer[E, ZE, T0, T],
                                                               writer2: Writer[E2, ZE, T0, T2]
                                                             ): Writer[E with E2, ZE, T0, (T, T2)] =
    new Writer[E with E2, ZE, T0, (T, T2)] {
      override def apply(i: ZStream[ZE, Throwable, T0]): ZIO[E with E2, Throwable, ZStream[ZE, Throwable, (T, T2)]] =
        for {
          s <- writer1(i)
          s2 <- writer2(i)
        } yield s.zip(s2)
    }

  //input stream will be passed to one writer then the output T will be passed to the second writer,
  // producing an output of T2
  def concatenate[E, E2, ZE, T0: ClassTag, T: ClassTag, T2: ClassTag](
                                         writer1: Writer[E, ZE, T0, T],
                                         writer2: Writer[E2, ZE, T, T2]
                                       ): Writer[E with E2, ZE, T0, T2] =
    new Writer[E with E2, ZE, T0, T2] {
      override def apply(i: ZStream[ZE, Throwable, T0]): ZIO[E with E2, Throwable, ZStream[ZE, Throwable, T2]] =
        for {
                s <- writer1(i)
                s2 <- writer2(s)
              } yield s2
    }

  class TTWriter[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                                                                   writer: Writer[E, ZE, T0, T1],
                                                                   transform:T1=>T2
                                                                 ) extends Writer[E, ZE, T0, T2] {
    override def apply(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = for {
      s <- writer.apply(i)
    } yield s.map(transform)
  }

  class ZTWriter[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                                                                   input: Writer[E, ZE, T0, T1],
                                                                   transform:ZStream[ZE, Throwable, T1] => ZStream[ZE, Throwable, T2]
                                                                 ) extends Writer[E, ZE, T0, T2]{
    override def apply(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = for {
      s <- input.apply(i)
    } yield transform(s)
  }

  class UnitZWriter[E, ZE, T: ClassTag, T2: ClassTag] (
                                                       t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2])
    extends Writer[E, ZE, T, T2]{
    override def apply(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = ZIO.succeed(t(i))
  }

  class UnitTWriter[E, ZE, T: ClassTag, T2: ClassTag] (t: T => T2)
    extends Writer[E, ZE, T, T2]{
    override def apply(i: ZStream[ZE, Throwable, T]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] =
      ZIO.succeed(i.map(t))
  }
}


