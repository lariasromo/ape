package ape.clickhouse.models

import scala.reflect.ClassTag

abstract class ClickhouseDLQModel[DLQ<:ClickhouseModel :ClassTag] extends ClickhouseModel {
  def dlq: DLQ
}
