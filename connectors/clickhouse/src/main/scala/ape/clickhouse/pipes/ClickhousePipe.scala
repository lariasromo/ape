package ape.clickhouse.pipes

import ape.pipe.Pipe

import scala.reflect.ClassTag

abstract class ClickhousePipe[E, ET, T:ClassTag, T1:ClassTag] extends Pipe[E, ET, T, T1]