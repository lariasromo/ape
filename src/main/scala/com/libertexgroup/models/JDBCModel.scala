package com.libertexgroup.models

import java.sql.PreparedStatement

trait JDBCModel {
  def sql: String
  def prepare(preparedStatement: PreparedStatement): Unit
}
