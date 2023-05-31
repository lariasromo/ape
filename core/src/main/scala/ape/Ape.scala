package ape

import ape.pipe.Pipe
import ape.reader.Reader
import zio._
import zio.stream.{ZSink, ZStream}

import scala.reflect.ClassTag

case class Ape[ZE, T: ClassTag] (stream: ZStream[ZE, Throwable, T]){
  def run: ZIO[ZE, Throwable, Unit] = stream.runDrain
  def run[R1, E1, L, Z](sink: => ZSink[R1, E1, T, L, Z]): ZIO[R1 with ZE, Any, Z] = stream.run(sink)
  def -->[E2, T2: ClassTag](writer: Pipe[E2, ZE, T, T2]): ZIO[E2, Throwable, Ape[ZE, T2]] =
    Ape.apply[E2, E2, ZE, T, T2](Reader.UnitReader(stream), writer)
}

object Ape {
  def readWrite[E, E2, ZE, T: ClassTag, T2: ClassTag](reader: Reader[E, ZE, T], writer: Pipe[E2, ZE, T, T2]):
  ZStream[ZE with E with E2, Throwable, T2] =
    ZStream.unwrap {
      for {
        s <- Ape.apply(reader, writer)
      } yield s.stream
    }

  def apply[E, E1, ZE, T0: ClassTag, T: ClassTag](
                               reader: Reader[E, ZE, T0],
                               writer: Pipe[E1, ZE, T0, T]
                             ): ZIO[E with E1, Throwable, Ape[ZE, T]] = {
    for {
      s <- reader.apply
      s2 <- writer.apply(s)
    } yield Ape(s2)
  }
}