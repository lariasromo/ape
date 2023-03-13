package com.libertexgroup.models.jdbc

import java.sql.PreparedStatement

trait JDBCModel {
  def sql: String

  def prepare(preparedStatement: PreparedStatement): Unit
}
