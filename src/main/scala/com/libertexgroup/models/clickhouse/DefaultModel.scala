package com.libertexgroup.models.clickhouse

import java.sql.PreparedStatement

case class DefaultModel(value: Array[Byte]) extends ClickhouseModel {
  override def sql: String = ???

  override def prepare(preparedStatement: PreparedStatement): Unit = ???
}
