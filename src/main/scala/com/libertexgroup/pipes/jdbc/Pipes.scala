package com.libertexgroup.pipes.jdbc

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs._
import com.libertexgroup.models.clickhouse.ClickhouseModel
import com.libertexgroup.models.jdbc.JDBCModel
import com.libertexgroup.utils.Utils.:=
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[pipes] class Pipes[Config <: JDBCConfig :Tag]() {
  def default[ET, Model <: JDBCModel :ClassTag]
  (implicit d1: ET := Any, d2: Model := JDBCModel):
    Pipe[Config, ET, Model, Chunk[Model]] = new DefaultPipe[ET, Config, Model]
}
