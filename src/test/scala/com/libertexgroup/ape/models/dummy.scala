package com.libertexgroup.ape.models

import com.libertexgroup.models.ClickhouseModel

import java.sql.{PreparedStatement, ResultSet}

case class dummy(a: String, b: String) extends ClickhouseModel {
  def this() = this("default", "default")

  override def sql: String = "INSERT INTO dummy(a, b) VALUES(?, ?);"

  override def prepare(preparedStatement: PreparedStatement): Unit = {
    preparedStatement.setString(1, a)
    preparedStatement.setString(2, b)
  }
}

object dummy {
  implicit val row2Object: ResultSet => dummy = rs => {
    dummy(
      rs.getString("a"),
      rs.getString("b")
    )
  }
}
