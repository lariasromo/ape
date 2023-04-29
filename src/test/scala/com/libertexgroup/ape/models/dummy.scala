package com.libertexgroup.ape.models

import com.datastax.oss.driver.api.core.cql
import com.datastax.oss.driver.api.core.cql.{BoundStatement, Row}
import com.libertexgroup.models.cassandra.CassandraModel
import com.libertexgroup.models.clickhouse.ClickhouseModel
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.apache.kafka.clients.consumer.ConsumerRecord

import java.sql.{PreparedStatement, ResultSet}

case class dummy(a: String, b: String) extends ClickhouseModel with CassandraModel {
  def this() = this("default", "default")

  override def sql: String = "INSERT INTO dummy(a, b) VALUES(?, ?);"

  override def prepare(preparedStatement: PreparedStatement): Unit = {
    preparedStatement.setString(1, a)
    preparedStatement.setString(2, b)
  }

  override def bind(preparedStatement: cql.PreparedStatement): BoundStatement = preparedStatement.bind(a, b)

  override def toString: String = s"$a, $b"
}

object dummy {
  implicit val jsonDecoder: Decoder[dummy] = deriveDecoder[dummy]
  implicit val jsonEncoder: Encoder[dummy] = deriveEncoder[dummy]

  implicit val str2Dummy: String => dummy = s => decode[dummy](s).toOption.orNull
  implicit val dummy2Str: dummy => String = s => s.asJson.noSpaces

  implicit val row2Object: ResultSet => dummy = rs => {
    dummy(
      rs.getString("a"),
      rs.getString("b")
    )
  }
  implicit val cassandraRow2Object: Row => dummy = rs => {
    dummy(
      rs.getString("a"),
      rs.getString("b")
    )
  }
  implicit val t: ConsumerRecord[String, Array[Byte]] => dummy = rec =>
    dummy(rec.value().map(_.toChar).mkString, rec.value().map(_.toChar).mkString)
}
