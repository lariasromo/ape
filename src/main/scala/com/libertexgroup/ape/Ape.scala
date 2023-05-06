package com.libertexgroup.ape

import com.libertexgroup.ape.Ape.Transition
import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.readers.PipelineReaders
import com.libertexgroup.pipes.Pipes
import zio._
import zio.stream.{ZSink, ZStream}

import scala.reflect.ClassTag

case class Ape[ZE, T: ClassTag] (stream: ZStream[ZE, Throwable, T], transitions: Seq[Transition]){
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
//        Disable banners
//        _ <- printLine(FigletFont.convertOneLine("APE"))
//        pipe = s.transitions.map(t => s"${t.t0}) -> ${t.op} -> (${t.t1}").mkString("-") + "|"
//        _ <- printLine("Forming a pipeline with transitions...")
//        _ <- printLine("-"*pipe.length)
//        _ <- printLine(pipe)
//        _ <- printLine("-"*pipe.length)
      } yield s.stream
    }


  case class Transition(t0: String, op:String, t1: String)
  def apply[E, E1, ZE, T0: ClassTag, T: ClassTag](
                               reader: Reader[E, ZE, T0],
                               writer: Pipe[E1, ZE, T0, T]
                             ): ZIO[E with E1, Throwable, Ape[ZE, T]] = {
    for {
      s <- reader.apply
      s2 <- writer.apply(s)
    } yield Ape(s2, reader.transitions ++ writer.transitions)
  }

  val readers = new PipelineReaders()
  val pipes = new Pipes()
}