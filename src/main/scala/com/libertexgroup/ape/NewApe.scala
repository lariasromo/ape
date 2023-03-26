package com.libertexgroup.ape

import com.libertexgroup.ape.readers.PipelineReaders
import com.libertexgroup.ape.writers.PipelineWriters
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

case class Ape[ZE, T] (stream: ZStream[ZE, Throwable, T]){
  def run: ZIO[ZE, Throwable, Unit] = stream.runDrain
}

object Ape {
  def apply[E, E1, ZE, T0, T](
                               reader: Reader[E, ZE, T0],
                               writer: Writer[E1, ZE, T0, T]
                             ): ZIO[E with E1, Throwable, Ape[ZE, T]] = for {
    s <- reader.apply
    s2 <- writer.apply(s)
  } yield Ape(s2)

  val readers = new PipelineReaders()
  val writers = new PipelineWriters()
}

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
          s <- writer1.apply(i)
          s2 <- writer2.apply(i)
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
                s <- writer1.apply(i)
                s2 <- writer2.apply(s)
              } yield s2
    }
}

abstract class Reader[E, ZE, T: ClassTag]{
  def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, T]]
  def read: ZIO[E, Throwable, ZStream[ZE, Throwable, T]] = apply
  def -->[E2, T2: ClassTag](writer: Writer[E2, ZE, T, T2]): ZIO[E with E2, Throwable, Ape[ZE, T2]] = Ape.apply(this, writer)
  
  def **[T2: ClassTag](implicit t: T => T2): Reader[E, ZE, T2] = new TTReader(this, t)
  def withTransform[T2: ClassTag](implicit t: T => T2): Reader[E, ZE, T2] = **
  
  def ***[T2: ClassTag](implicit t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Reader[E, ZE, T2] = 
    new ZTReader(this, t)
  def withZTransform[T2: ClassTag]
                    (implicit t: ZStream[ZE, Throwable, T] => ZStream[ZE, Throwable, T2]): Reader[E, ZE, T2] = ***
}

class TTWriter[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                                                                 writer: Writer[E, ZE, T0, T1],
                                                                 transform:T1=>T2
                                                               ) extends Writer[E, ZE, T0, T2] {
  override def apply(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = for {
    s <- writer.apply(i)
  } yield s.map(transform)
}

class TTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                                                   input: Reader[E, ZE, T0],
                                                   transform:T0=>T1
                                                 ) extends Reader[E, ZE, T1] {
  override def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, T1]] = for {
    s <- input.apply
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

class ZTReader[E, ZE, T0: ClassTag, T1: ClassTag](
                                                   input: Reader[E, ZE, T0],
                                                   transform:ZStream[ZE, Throwable, T0] => ZStream[ZE, Throwable, T1]
                                                 ) extends Reader[E, ZE, T1]{
  override def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, T1]] = for {
    s <- input.apply
  } yield transform(s)
}

class UnitReader[ZE, T: ClassTag] (stream: ZStream[ZE, Throwable, T]) extends Reader[Any, ZE, T]{
  override def apply: ZIO[Any, Throwable, ZStream[ZE, Throwable, T]] = ZIO.succeed(stream)
}

