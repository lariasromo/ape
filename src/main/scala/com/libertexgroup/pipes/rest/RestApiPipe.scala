package com.libertexgroup.pipes.rest

import com.libertexgroup.ape.pipe.Pipe

import scala.reflect.ClassTag

abstract class RestApiPipe[E, E1, T :ClassTag, T2 :ClassTag] extends Pipe[E, E1, T, T2] {
  override val name: String = "RestApiWriter"
}