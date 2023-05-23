package ape.jdbc.pipes

import ape.pipe.Pipe

import scala.reflect.ClassTag

abstract class JDBCPipe[E, E1, T :ClassTag, T2 :ClassTag] extends Pipe[E, E1, T, T2]