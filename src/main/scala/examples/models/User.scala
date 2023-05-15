package examples.models

import com.libertexgroup.models.clickhouse.ClickhouseModel
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.apache.kafka.clients.producer.ProducerRecord

import java.sql.PreparedStatement

case class User(
                 firstName: String,
                 lastName: String,
                 country: String
               ) extends ClickhouseModel {
  override def sql = "INSERT INTO Users(firstName, lastName, country) VALUES(?, ?, ?)"

  override def prepare(preparedStatement: PreparedStatement): Unit = {
    preparedStatement.setString(1, firstName)
    preparedStatement.setString(2, lastName)
    preparedStatement.setString(3, country)
  }
}

object User {
  implicit val jsonDecoder: Decoder[User] = deriveDecoder[User]
  implicit val jsonEncoder: Encoder[User] = deriveEncoder[User]
  val toKafka: User => ProducerRecord[String, User] =
    user => new ProducerRecord[String, User]("key", user)
}