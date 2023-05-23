package ape.cassandra.models

import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}

trait CassandraModel {
  def sql: String

  def bind(preparedStatement: PreparedStatement): BoundStatement
}
