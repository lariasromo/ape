package ape.clickhouse.models

import java.sql.PreparedStatement

trait ClickhouseModel {
  def sql: String

  def prepare(preparedStatement: PreparedStatement): Unit
}
