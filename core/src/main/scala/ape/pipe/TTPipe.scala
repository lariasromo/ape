package ape.pipe

import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag


class TTPipe[E, ZE, T0: ClassTag, T1: ClassTag, T2: ClassTag](
                                                               writer: Pipe[E, ZE, T0, T1],
                                                               transform:T1=>T2,
                                                               n:String="TTWriter"
                                                               ) extends Pipe[E, ZE, T0, T2] {
    override protected[this] def pipe(i: ZStream[ZE, Throwable, T0]): ZIO[E, Throwable, ZStream[ZE, Throwable, T2]] = for {
    s <- writer.apply(i)
  } yield s.map(transform)

  override def name: String = writer.name
}
