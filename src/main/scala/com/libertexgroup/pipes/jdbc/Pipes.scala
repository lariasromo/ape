package com.libertexgroup.pipes.jdbc

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs._
import com.libertexgroup.models.jdbc.JDBCModel
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[pipes] class Pipes[Config <: JDBCConfig :Tag]() {
  def default[ET, Model <: JDBCModel :ClassTag]: Pipe[Config, ET, Model, Chunk[Model]] = new DefaultPipe[ET, Config, Model]
}
