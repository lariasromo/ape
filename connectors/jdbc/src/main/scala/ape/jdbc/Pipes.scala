package ape.jdbc

import ape.jdbc.configs.JDBCConfig
import ape.jdbc.models.JDBCModel
import ape.jdbc.pipes.DefaultPipe
import ape.pipe.Pipe
import ape.utils.Utils.:=
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[jdbc] class Pipes[Config <: JDBCConfig :Tag]() {
  def default[ET, Model <: JDBCModel :ClassTag]
  (implicit d1: ET := Any, d2: Model := JDBCModel):
  Pipe[Config, ET, Model, Chunk[Model]] = new DefaultPipe[ET, Config, Model]
}

object Pipes {
  def pipes[Config <: JDBCConfig :Tag](implicit d: Config := JDBCConfig) = new Pipes[Config]()
}