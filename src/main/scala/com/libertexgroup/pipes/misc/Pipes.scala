package com.libertexgroup.pipes.misc

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.utils.Utils.:=
import zio.Queue

import scala.reflect.ClassTag

protected[pipes] class Pipes() {
  class queue[E, ZE]{
    def of[T: ClassTag](queue:Queue[T]): QueuePipe[E, ZE, T] = new QueuePipe[E, ZE, T](queue)
  }
  def queue[E, ZE](implicit d1: E := Any, d2: ZE := Any) = new queue[E, ZE]

  class console[E, ZE]{
    def of[T: ClassTag]: Pipe[E, ZE, T, T] = new ConsolePipe[E, ZE, T]
    def ofString: Pipe[E, ZE, String, String] = new ConsolePipe[E, ZE, String]
  }
  def console[E, ZE](implicit d1: E := Any, d2: ZE := Any) = new console[E, ZE]

  class backPressure[ZE]{
    def finite[T: ClassTag] = new BackPressureFinitePipe[ZE, T]
    def infinite[T: ClassTag] = new BackPressureInfinitePipe[ZE, T]
  }
  def backPressure[ZE](implicit d1: ZE := Any) = new backPressure[ZE]
}
