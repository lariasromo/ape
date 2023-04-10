package com.libertexgroup.ape.writers.clickhouse

import com.libertexgroup.ape.Writer
import com.libertexgroup.configs._
import com.libertexgroup.models.clickhouse.ClickhouseModel
import zio.{Chunk, Tag}

import scala.reflect.ClassTag

protected[writers] class Writers[Config <: MultiClickhouseConfig :Tag]() {
  def default[ET, T <:ClickhouseModel :ClassTag]: Writer[Config, ET, T, Chunk[(T, Int)]] =
    new DefaultWriter[ET, T, Config]
}
