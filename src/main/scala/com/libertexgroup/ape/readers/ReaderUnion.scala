package com.libertexgroup.ape.readers

import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class ReaderUnion[E, E1, T: ClassTag, EE, EE1, TT: ClassTag, TTT: ClassTag](
                                                                   reader1: Reader[E, E1, T],
                                                                   reader2: Reader[EE, EE1, TT],
  streamTransformer: (ZStream[E1, Throwable, T], ZStream[EE1, Throwable, TT]) => ZStream[E1 with EE1, Throwable, TTT]
                                                                 )
 extends Reader[E with EE, E1 with EE1, TTT]{
  override def apply: ZIO[E with EE, Throwable, ZStream[E1 with EE1, Throwable, TTT]] = for {
    s1 <- reader1.apply
    s2 <- reader2.apply
  } yield streamTransformer(s1, s2)
}
