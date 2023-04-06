package com.libertexgroup.ape.writers.jdbc

import com.libertexgroup.configs._
import com.libertexgroup.models.jdbc.JDBCModel
import zio.Tag

import scala.reflect.ClassTag

protected[writers] class Writers[Config <: JDBCConfig :Tag]() {
  def default[ET, Model <: JDBCModel :ClassTag] = new DefaultWriter[ET, Config, Model]
}
