package com.libertexgroup.ape.writers.jdbc

import com.libertexgroup.ape.Writer
import com.libertexgroup.configs._
import com.libertexgroup.models.jdbc.JDBCModel
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[writers] class Writers[Config <: JDBCConfig :Tag]() {
  def default[ET, Model <: JDBCModel :ClassTag]: Writer[Config, ET, Model, Chunk[Model]] = new DefaultWriter[ET, Config, Model]
}
