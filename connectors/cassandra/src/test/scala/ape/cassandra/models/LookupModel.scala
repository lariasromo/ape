package ape.cassandra.models
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement, Row}

case class LookupModel(value:String) extends CassandraLookupModel[dummy] {
  override def lookupQuery: String = "select a, b from dummy where a = ?"

  override def lookupBind(preparedStatement: PreparedStatement): BoundStatement = preparedStatement.bind(value)

  override def lookupDecode(row: Row): dummy = dummy(row.getString("a"), row.getString("b"))
}
