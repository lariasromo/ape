package ape.cassandra.models

import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement, Row}

trait CassandraLookupModel[T] {
  def lookupQuery: String

  def lookupBind(preparedStatement: PreparedStatement): BoundStatement

  def lookupDecode(row: Row): T
}
