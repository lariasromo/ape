package ape.clickhouse.models

import zio.{Duration, durationInt}

import java.sql.{PreparedStatement, ResultSet}

trait ClickhouseLookupModel[T] {
  def lookupQuery: String

  def lookupBind(preparedStatement: PreparedStatement): Unit

  def lookupDecode(row: ResultSet): T

  lazy val timeout: Duration = 10.seconds
}
