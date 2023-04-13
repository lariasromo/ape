package com.libertexgroup.ape.writers.misc

import com.libertexgroup.ape.Writer
import zio.Queue

import scala.reflect.ClassTag

protected[writers] class Writers() {
  def queue[E, ZE, T: ClassTag](queue:Queue[T]): QueueWriter[E, ZE, T] = new QueueWriter[E, ZE, T](queue)

  def console[E, ET, T: ClassTag]: Writer[E, ET, T, T] = new ConsoleWriter[E, ET, T]

  def consoleString[E, ET]: Writer[E, ET, String, String] = new ConsoleWriter[E, ET, String]
}
