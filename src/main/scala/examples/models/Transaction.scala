package examples.models

import com.datastax.oss.driver.api.core.cql
import com.libertexgroup.models.cassandra.CassandraModel
import com.libertexgroup.models.jdbc.JDBCModel

import java.sql.PreparedStatement

case class Transaction(status: String, transactionType: String, clientId: String) extends CassandraModel with JDBCModel {
  override def sql: String = "INSERT INTO transactions(status, transactionType, clientId) VALUES(?, ?, ?);"

  override def bind(preparedStatement: cql.PreparedStatement): cql.BoundStatement =
    preparedStatement.bind(status, transactionType, clientId)

  override def prepare(preparedStatement: PreparedStatement): Unit = {
    preparedStatement.setString(1, status)
    preparedStatement.setString(2, transactionType)
    preparedStatement.setString(3, clientId)
  }
}