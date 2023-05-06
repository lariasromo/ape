package com.libertexgroup.pipes.misc

import com.libertexgroup.ape.pipe.Pipe
import zio.Queue

import scala.reflect.ClassTag

protected[pipes] class Pipes() {
  def queue[E, ZE, T: ClassTag](queue:Queue[T]): QueuePipe[E, ZE, T] = new QueuePipe[E, ZE, T](queue)

  def console[E, ET, T: ClassTag]: Pipe[E, ET, T, T] = new ConsolePipe[E, ET, T]

  def consoleString[E, ET]: Pipe[E, ET, String, String] = new ConsolePipe[E, ET, String]

  def backPressureFinite[ZE, T: ClassTag] = new BackPressureFinitePipe[ZE, T]

  def backPressureInfinite[ZE, T: ClassTag] = new BackPressureInfinitePipe[ZE, T]
}
