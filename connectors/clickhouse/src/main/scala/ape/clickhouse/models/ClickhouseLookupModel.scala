package ape.clickhouse.models

import zio.{Duration, durationInt}

import java.sql.{PreparedStatement, ResultSet}

trait ClickhouseLookupModel[T] {
  def lookupQuery: String

  def lookupBind(preparedStatement: PreparedStatement): Unit

  val lookupDecode: ResultSet => T

  lazy val timeout: Duration = 10.seconds
}
